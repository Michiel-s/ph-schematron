/**
 * Copyright (C) 2014-2017 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.schematron.supplementary;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.oclc.purl.dsdl.svrl.SchematronOutputType;

import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.schematron.svrl.SVRLMarshaller;
import com.helger.schematron.xslt.SchematronResourceSCH;
import com.helger.xml.serialize.write.XMLWriter;

public final class Issue8Test
{
  @Test
  public void testIssue () throws Exception
  {
    validateAndProduceSVRL (new File ("src/test/resources/issues/github8/schematron.sch"),
                            new File ("src/test/resources/issues/github8/test.xml"));
  }

  public static void validateAndProduceSVRL (@Nonnull final File aSchematron, final File aXML) throws Exception
  {
    // Create the custom parameters
    final ICommonsMap <String, Object> aCustomParameters = new CommonsHashMap <> ();
    aCustomParameters.put ("xyz", "mobile");
    aCustomParameters.put ("expected", "");

    final SchematronResourceSCH aSCH = SchematronResourceSCH.fromFile (aSchematron);

    // Assign custom parameters
    aSCH.setParameters (aCustomParameters);

    if (false)
      System.out.println (XMLWriter.getNodeAsString (aSCH.getXSLTProvider ().getXSLTDocument ()));

    // Perform validation
    final SchematronOutputType aSVRL = aSCH.applySchematronValidationToSVRL (new FileSystemResource (aXML));
    assertNotNull (aSVRL);
    if (false)
      System.out.println (new SVRLMarshaller ().getAsString (aSVRL));
  }
}
