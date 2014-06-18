/*
 * Copyright 2005-2011 by BerryWorks Software, LLC. All rights reserved.
 *
 * This file is part of EDIReader. You may obtain a license for its use directly from
 * BerryWorks Software, and you may also choose to use this software under the terms of the
 * GPL version 3. Other products in the EDIReader software suite are available only by licensing
 * with BerryWorks. Only those files bearing the GPL statement below are available under the GPL.
 *
 * EDIReader is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * EDIReader is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EDIReader.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package com.berryworks.edireader.tokenizer;

import com.berryworks.edireader.EDIReader;
import com.berryworks.edireader.EDISyntaxException;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

/**
 * Interprets EDI input as a sequence of primitive syntactic tokens.
 * <p/>
 * As an EDI interchange is parsed, the parser uses a Tokenizer to advance through the
 * input EDI stream one token at a time. A call to <code>nextToken()</code> causes the tokenizer to advance
 * past the next token and return a <code>Token</code> instance describing that token.
 * <p/>
 * This implementation of Tokenizer uses CharBuffer instead of char[].
 * <p.>
 */
public class EDITokenizerNIO extends AbstractTokenizer
{

  public static final int BUFFER_SIZE = 1000;
  private final CharBuffer charBuffer = CharBuffer.wrap(new char[BUFFER_SIZE]);

  public EDITokenizerNIO(Reader source)
  {
    super(source);
    charBuffer.flip();
    if (EDIReader.debug)
      trace("Constructed a new EDITokenizer");
  }

  public EDITokenizerNIO(Reader source, char[] preRead)
  {
    this(source);
    if (preRead == null || preRead.length == 0)
      return;

    if (preRead.length > charBuffer.capacity())
      throw new RuntimeException("Attempt to create EDITokenizer with " + preRead.length +
        " pre-read chars, which is greater than the internal buffer size of " + charBuffer.capacity());
    charBuffer.clear();
    charBuffer.put(preRead);
    charBuffer.flip();
  }

  /**
   * Returns a String representation of the current state of the tokenizer
   * for testing and debugging purposes.
   *
   * @return String representation
   */
  @Override
  public String toString()
  {
    String result = "tokenizer state:";
    result += " segmentCount=" + segmentCount;
    result += " charCount=" + charCount;
    result += " segTokenCount=" + segTokenCount;
    result += " segCharCount=" + segCharCount;
    result += " currentToken=" + currentToken;
    result += " buffer.limit=" + charBuffer.limit();
    result += " buffer.position=" + charBuffer.position();
    return result;
  }

  /**
   * Gets the next character of input. <pr>Sets cChar, cClass
   *
   * @throws java.io.IOException for problem reading EDI data
   */
  public void getChar() throws IOException
  {
    if (unGot)
    {
      // The current character has been "put back" with ungetChar()
      // after having been seen with getChar(). Therefore, this call
      // to getChar() can simply reget the current character.
      unGot = false;
      charCount++;
      segCharCount++;
      return;
    }

    // Read a fresh character from the input source.
    // But first copy the current one to an outputWriter
    // or the recorder if necessary.
    if (outputWriter != null)
    {
      // We do have an outputWriter wanting data, but do we have
      // a current character to write? And make sure writing is
      // not suspended.
      if ((!endOfFile) && (!writingSuspended))
        outputWriter.write(cChar);
    }
    if (recorderOn)
      recording.append(cChar);

    if (charBuffer.remaining() == 0)
    {
      readUntilBufferProvidesAtLeast(1);
    }

    if (endOfFile)
    {
      cClass = CharacterClass.EOF;
      if (EDIReader.debug)
        trace("end-of-file encountered");
    }
    else
    {
      cChar = charBuffer.get();
      if (cChar == delimiter)
        cClass = CharacterClass.DELIMITER;
      else if (cChar == subDelimiter)
        cClass = CharacterClass.SUB_DELIMITER;
      else if (cChar == release)
        cClass = CharacterClass.RELEASE;
      else if (cChar == terminator)
        cClass = CharacterClass.TERMINATOR;
      else if (cChar == repetitionSeparator)
        cClass = CharacterClass.REPEAT_DELIMITER;
      else
        cClass = CharacterClass.DATA;
    }
    charCount++;
    segCharCount++;
  }

  public char[] getBuffered()
  {
    char[] result = new char[0];

    if (endOfFile)
      return result;

    if (charBuffer.remaining() == 0 && !unGot)
    {
      return result;
    }

    try
    {
      result = lookahead(charBuffer.remaining() + (unGot ? 1 : 0));
    } catch (Exception ignore)
    {
    }

    return result;
  }

  /**
   * Look ahead into the source of input chars and return the next n chars to
   * be seen, without disturbing the normal operation of getChar().
   *
   * @param n number of chars to return
   * @return char[] containing upcoming input chars
   * @throws java.io.IOException for problem reading EDI data
   * @throws com.berryworks.edireader.EDISyntaxException
   *
   */
  public char[] lookahead(int n) throws IOException, EDISyntaxException
  {
    if (EDIReader.debug)
      trace("EDITokenizer.lookahead(" + n + ")");

    char[] rval = new char[n];

    // The 1st char is grabbed using the tokenizer's built-in
    // getChar() / ungetChar() mechanism. This allows things to work
    // properly whether or not the next char has already been gotten.
    getChar();
    rval[0] = cChar;
    ungetChar();

    // The minus 1 is because we have already filled the first char of the return value, so we only need n-1 more
    if (charBuffer.remaining() < n - 1)
    {
      if (EDIReader.debug)
        if (EDIReader.debug)
          trace("buffering more data to satisfy lookahead(" + n + ")");
      charBuffer.compact();
      readUntilBufferProvidesAtLeast(n - 1);
    }

    // Move chars from the buffer into the return value,
    // up to the length of the buffer
    int j = 1;
    for (int i = charBuffer.position(); i < charBuffer.position() + n - 1; i++)
      rval[j++] = charBuffer.get(i);

    // If more lookahead chars were requested than were satisfied for any reason,
    // then fill the return value with '?' to the requested length.
    for (; j < n;) rval[j++] = '?';

    return rval;
  }

  private void readUntilBufferProvidesAtLeast(int needed) throws IOException
  {

    while (charBuffer.remaining() < needed)
    {
      charBuffer.compact();
      int n;
      while ((n = inputReader.read(charBuffer)) == 0)
      {
        if (EDIReader.debug) trace("read returned zero in readUntil...");
      }
      if (EDIReader.debug) trace("readUntil... got " + n + " chars of input into buffer");
      if (n < 0)
      {
        if (EDIReader.debug) trace("hit end of file in readUntil...");
        endOfFile = true;
        break;
      }
      charBuffer.flip();
    }
  }


}