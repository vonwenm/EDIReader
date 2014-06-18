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

package com.berryworks.edireader;

import com.berryworks.edireader.util.BranchingWriter;
import com.berryworks.edireader.util.DateTimeGenerator;

import java.io.IOException;

/**
 * A delegate for generating an interchange containing some number of 997
 * transactions acknowledging the functional groups parsed by AnsiReader.
 */
public class AnsiFAGenerator extends ReplyGenerator
{

  private final BranchingWriter ackStream;
  private boolean preambleGenerated, skipFA;
  private final String thisGroupControlNumber;
  private int thisDocumentCount;
  private String referencedISA;
  private boolean headerGenerated;
  private boolean groupTrailerGenerated;
  private char delimiter;
  private String terminatorWithSuffix;

  private static final String CONTROL_NUMBER_997 = "0001";

  public AnsiFAGenerator(final StandardReader ansiReader, final BranchingWriter ackStream)
  {
    this.standardReader = ansiReader;
    this.ackStream = ackStream;
    thisGroupControlNumber = "12345";
  }

  @Override
  public void generateAcknowledgmentHeader(String firstSegment,
                                           String groupSender, String groupReceiver, int groupDateLength,
                                           String groupVersion, String groupFunctionCode,
                                           String groupControlNumber) throws IOException
  {
    if (ackStream == null)
      return;

    // Do not generate an FA to acknowledge an FA
    if ("FA".equals(groupFunctionCode))
    {
      skipFA = true;
      return;
    }
    skipFA = false;

    if (EDIReader.debug) EDIReader.trace("generating FA envelope");
    generateAcknowledgementPreamble(firstSegment, groupSender,
      groupReceiver, groupDateLength, groupVersion);

    // Generate the ST 997
    if (EDIReader.debug) EDIReader.trace("generating first part of 997");
    thisDocumentCount++;
    ackStream.write("ST" + delimiter + "997" + delimiter + CONTROL_NUMBER_997);
    ackStream.write(terminatorWithSuffix);

    // Generate the AK1 segment to identify the group being acknowledged
    ackStream.write("AK1" + delimiter + groupFunctionCode + delimiter
      + groupControlNumber);
    ackStream.write(terminatorWithSuffix);
    headerGenerated = true;
  }

  @Override
  public void generateTransactionAcknowledgment(String transactionCode,
                                                String controlNumber) throws IOException
  {
    if (ackStream == null || skipFA)
      return;

    if (EDIReader.debug) EDIReader.trace("generating AK2/AK5");
    // Generate the AK2 segment to identify the transaction set
    ackStream.writeTrunk("AK2" + delimiter + transactionCode + delimiter
      + controlNumber);
    ackStream.writeTrunk(terminatorWithSuffix);

    // Generate the AK5 segment acknowledging the transaction set
    ackStream.writeTrunk("AK5" + delimiter + "A");
    ackStream.writeTrunk(terminatorWithSuffix);
  }

  @Override
  public void generateGroupAcknowledgmentTrailer(int docCount)
    throws IOException
  {
    if (ackStream == null || skipFA)
      return;

    if (EDIReader.debug) EDIReader.trace("generating AK9, SE");
    // For the trunk, generate the AK9 segment to designate acceptance of the entire
    // functional group.
    ackStream.writeTrunk("AK9" + delimiter + "A" + delimiter + docCount
      + delimiter + docCount + delimiter + docCount);
    // For the branch, generate the AK9 segment to designate rejection of the entire
    // functional group.
    ackStream.writeBranch("AK9" + delimiter + "R" + delimiter + docCount
      + delimiter + docCount + delimiter + "0");
    ackStream.write(terminatorWithSuffix);

    // Generate the SE to match the ST
    ackStream.writeTrunk("SE" + delimiter + (4 + 2 * docCount) + delimiter + CONTROL_NUMBER_997);
    ackStream.writeBranch("SE" + delimiter + "4" + delimiter + CONTROL_NUMBER_997);
    ackStream.write(terminatorWithSuffix);
    groupTrailerGenerated = true;
  }

  @Override
  public void generateNegativeACK() throws IOException
  {
    if (ackStream == null || skipFA || !headerGenerated)
      return;

    if (EDIReader.debug) EDIReader.trace("recasting 997 as negative");
    if (!groupTrailerGenerated)
      generateGroupAcknowledgmentTrailer(0);
    generateAcknowledgementWrapup(false);
  }

  @Override
  public void generateAcknowledgementWrapup() throws IOException
  {
    generateAcknowledgementWrapup(true);
  }

  public void generateAcknowledgementWrapup(boolean positiveFA) throws IOException
  {
    if (ackStream == null || skipFA)
      return;

    if (EDIReader.debug) EDIReader.trace("generating GE, IEA");
    // Generate the GE to match the GS
    ackStream.write("GE" + delimiter + thisDocumentCount + delimiter
      + thisGroupControlNumber);
    ackStream.write(terminatorWithSuffix);

    // Finish with an IEA corresponding to the ISA
    ackStream.write("IEA" + delimiter + "1" + delimiter
      + referencedISA.substring(90, 99));
    ackStream.write(terminatorWithSuffix);
    if (positiveFA)
      ackStream.close();
    else
      ackStream.closeUsingBranch();
  }

  private void generateAcknowledgementPreamble(String firstSegment,
                                               String groupSender, String groupReceiver, int groupDateLength,
                                               String groupVersion) throws IOException
  {
    if (ackStream == null || preambleGenerated)
      return;

    referencedISA = firstSegment;

    // Note that the initialization of the following items cannot occur
    // in the constructor because ansiReader may not have all of the
    // necessary information at that point.
    establishSyntaxCharacters();

    // The ISA envelope is basically the same as that of the input
    // interchange except for reversal of the sender and receiver addresses.
    String sender = firstSegment.substring(51, 70);
    String receiver = firstSegment.substring(32, 51);
    String faHeader = firstSegment.substring(0, 32) + sender + receiver
      + DateTimeGenerator.generate(delimiter)
      + firstSegment.substring(81, firstSegment.length() - 1);

    char senderDelimiter = standardReader.getDelimiter();
    if (senderDelimiter != delimiter)
      faHeader = faHeader.replace(senderDelimiter, delimiter);

    ackStream.write(faHeader);
    ackStream.write(terminatorWithSuffix);

    if (standardReader.isInterchangeAcknowledgment())
    {
      ackStream.write("TA1" + delimiter +
        firstSegment.substring(90, 99) + delimiter +
        firstSegment.substring(70, 76) + delimiter +
        firstSegment.substring(77, 81) + delimiter);
      ackStream.writeTrunk("A" + delimiter + "000");
      ackStream.writeBranch("R" + delimiter + "022");
      ackStream.write(terminatorWithSuffix);
    }

    ackStream.write("GS" + delimiter + "FA" + delimiter + groupReceiver
      + delimiter + groupSender + delimiter
      + controlDateAndTime(groupDateLength, delimiter) + delimiter
      + thisGroupControlNumber + delimiter + "X" + delimiter
      + groupVersion);
    ackStream.write(terminatorWithSuffix);

    preambleGenerated = true;
  }

  private void establishSyntaxCharacters()
  {
    SyntaxDescriptor sd = standardReader.getAcknowledgmentSyntaxDescriptor();
    char terminator = 0;
    String terminatorSuffix = null;
    if (sd == null)
    {
      delimiter = 0;
    }
    else
    {
      delimiter = sd.getDelimiter();
      terminator = sd.getTerminator();
      terminatorSuffix = sd.getTerminatorSuffix();
    }

    if (delimiter == 0)
      delimiter = standardReader.getDelimiter();

    if (terminator == 0)
      terminator = standardReader.getTerminator();

    if (terminatorSuffix == null)
      terminatorSuffix = standardReader.getTerminatorSuffix();

    terminatorWithSuffix = terminator + terminatorSuffix;
  }

  @Override
  public void generateAcknowledgmentHeader(String syntaxIdentifier,
                                           String syntaxVersionNumber, String fromId, String fromQual,
                                           String toId, String toQual, String interchangeControlNumber)
  {
  }

}
