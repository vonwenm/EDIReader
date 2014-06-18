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

package com.berryworks.edireader.demo;

import com.berryworks.edireader.EDIReader;
import com.berryworks.edireader.EDIReaderFactory;
import com.berryworks.edireader.TransactionCallback;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;

public class AnsiTransactionExtractor
{
  private InputSource inputSource;

  public AnsiTransactionExtractor(String inputFileName)
  {

    try
    {
      inputSource = new InputSource(new FileReader(inputFileName));
    } catch (IOException e)
    {
      System.out.println(e.getMessage());
    }
  }

  public void run()
  {

    try
    {
      while (true)
      {
        EDIReader parser = EDIReaderFactory.createEDIReader(inputSource);
        if (parser == null)
          break;

        /**
         * This is the interesting part where we arrange to get the original EDI text
         * for each ST...SE sequence contained with the EDI interchange(s).
         */
        parser.setTransactionCallback(new MyCallback(parser));

        /**
         * In this demo, we do not care what happens to the XML generated by EDIReader,
         * so just set a DefaultHandler which is a do-nothing handler.
         */
        parser.setContentHandler(new DefaultHandler());

        parser.parse(inputSource);
      }

    } catch (IOException e)
    {
      System.out.println(e.getMessage());
    } catch (SAXException e)
    {
      System.out.println("EDI input not well-formed: " + e.toString());
    }
  }

  static class MyCallback implements TransactionCallback
  {
    private final EDIReader parser;
    StringWriter stringWriter;

    public MyCallback(EDIReader parser)
    {
      this.parser = parser;
    }

    public void startTransaction(String segmentType)
    {
      stringWriter = new StringWriter();
      /**
       * Note that the parser does not start copying content to the
       * copy writer until after the ST token has been noted. Therefore,
       * if you want "ST" to appear in your captured output, you can
       * place it there at this point. The parser provides it as an
       * argument for your convenience.
       */
      stringWriter.write(segmentType);
      parser.setCopyWriter(stringWriter);
    }

    public void endTransaction()
    {
      parser.setCopyWriter(null);
      String transactionString = stringWriter.toString();
      System.out.println("--- ST/SE Sequence ----------------------");
      System.out.println(transactionString);
      System.out.println("-----------------------------------------");
    }
  }

  public static void main(String args[])
  {
    if (args.length < 1)
      badArgs();

    AnsiTransactionExtractor demo = new AnsiTransactionExtractor(args[0]);
    demo.run();
  }

  private static void badArgs()
  {
    System.out.println("Usage: AnsiTransactionExtractor inputfile");
    throw new RuntimeException("Missing or invalid command line arguments");
  }

}