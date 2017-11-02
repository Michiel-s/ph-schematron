/**
 * Copyright (C) 2017 Philip Helger (www.helger.com)
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
package com.helger.schematron.ant;

import java.io.File;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.transform.URIResolver;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.XMLCatalog;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.ResourceUtils;
import org.oclc.purl.dsdl.svrl.SchematronOutputType;
import org.xml.sax.EntityResolver;

import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.error.ErrorTextProvider;
import com.helger.commons.error.IError;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.error.list.IErrorList;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.string.StringHelper;
import com.helger.schematron.ESchematronMode;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.pure.SchematronResourcePure;
import com.helger.schematron.pure.errorhandler.CollectingPSErrorHandler;
import com.helger.schematron.svrl.AbstractSVRLMessage;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.schematron.svrl.SVRLMarshaller;
import com.helger.schematron.xslt.SchematronResourceSCH;
import com.helger.schematron.xslt.SchematronResourceXSLT;
import com.helger.xml.transform.CollectingTransformErrorListener;
import com.helger.xml.transform.TransformSourceFactory;

/**
 * ANT task to perform Schematron validation.
 *
 * @author Philip Helger
 * @since 4.2.3
 */
public class Schematron extends Task
{
  /**
   * The Schematron file. This may also be an XSLT file if it is precompiled.
   */
  private File m_aSchematronFile;

  /**
   * The processing engine to use. Can be one of the following:
   * <ul>
   * <li>pure - for SCH files</li>
   * <li>schematron - for SCH files that will be converted to XSLT and applied
   * from there.</li>
   * <li>xslt - apply pre-build XSLT files</li>
   * </ul>
   */
  private ESchematronMode m_eSchematronProcessingEngine = ESchematronMode.SCHEMATRON;

  /**
   * The collection for resources (like FileSets etc.) which are to be
   * validated.
   */
  private final ICommonsList <ResourceCollection> m_aResCollections = new CommonsArrayList <> ();

  /**
   * The SVRL path to write to. The filenames are based on the source XML
   * filenames.
   */
  private File m_aSvrlDirectory;

  /**
   * Define the phase to be used for Schematron validation. By default the
   * <code>defaultPhase</code> attribute of the Schematron file is used. This
   * phase name is only used if the processing engine <code>pure</code> or
   * <code>schematron</code> are used.
   */
  private String m_sPhaseName;

  /**
   * Define the language code to be used for Schematron validation. Default is
   * English. Supported language codes are: cs, de, en, fr, nl.
   */
  private String m_sLanguageCode;

  /**
   * <code>true</code> if the XMLs are supposed to be valid, <code>false</code>
   * otherwise. Defaults to <code>true</code>.
   */
  private boolean m_bExpectSuccess = true;

  /**
   * <code>true</code> if the build should fail if any error occurs. Defaults to
   * <code>true</code>. Since v5.0.0.
   */
  private boolean m_bFailOnError = true;

  /**
   * For resolving entities such as DTDs. This is used both for the Schematron
   * file as well as for the XML files to be validated.
   */
  private final XMLCatalog m_aXmlCatalog = new XMLCatalog ();

  public Schematron ()
  {}

  public void setSchematronFile (@Nonnull final File aFile)
  {
    m_aSchematronFile = aFile;
    if (!m_aSchematronFile.isAbsolute ())
      m_aSchematronFile = new File (getProject ().getBaseDir (), aFile.getPath ());
    log ("Using Schematron file '" + m_aSchematronFile + "'", Project.MSG_DEBUG);
  }

  public void setSchematronProcessingEngine (@Nullable final String sEngine)
  {
    m_eSchematronProcessingEngine = ESchematronMode.getFromIDOrNull (sEngine);
    log ("Schematron processing mode set to '" + m_eSchematronProcessingEngine + "'", Project.MSG_DEBUG);
  }

  /**
   * Add a collection of files to copy.
   *
   * @param aResCollection
   *        a resource collection to copy.
   * @since Ant 1.7
   */
  public void add (final ResourceCollection aResCollection)
  {
    m_aResCollections.add (aResCollection);
  }

  public void setSvrlDirectory (@Nonnull final File aDir)
  {
    m_aSvrlDirectory = aDir;
    if (!m_aSvrlDirectory.isAbsolute ())
      m_aSvrlDirectory = new File (getProject ().getBaseDir (), aDir.getPath ());
    log ("Writing SVRL files to directory '" + m_aSvrlDirectory + "'", Project.MSG_DEBUG);
  }

  public void setPhaseName (@Nullable final String sPhaseName)
  {
    m_sPhaseName = sPhaseName;

    if (m_sPhaseName == null)
      log ("Using default phase", Project.MSG_DEBUG);
    else
      log ("Using the phase '" + m_sPhaseName + "'", Project.MSG_DEBUG);
  }

  public void setLanguageCode (@Nullable final String sLanguageCode)
  {
    m_sLanguageCode = sLanguageCode;

    if (m_sLanguageCode == null)
      log ("Using default language code", Project.MSG_DEBUG);
    else
      log ("Using the language code '" + m_sLanguageCode + "'", Project.MSG_DEBUG);
  }

  public void setExpectSuccess (final boolean bExpectSuccess)
  {
    m_bExpectSuccess = bExpectSuccess;

    log ("Expecting that XML files " +
         (bExpectSuccess ? "conform" : "do not conform") +
         " to the provided Schematron file",
         Project.MSG_DEBUG);
  }

  public void setFailOnError (final boolean bFailOnError)
  {
    m_bFailOnError = bFailOnError;

    log (bFailOnError ? "Will fail on error" : "Will not fail on error", Project.MSG_DEBUG);
  }

  /**
   * Add the catalog to our internal catalog
   *
   * @param aXmlCatalog
   *        the XMLCatalog instance to use to look up DTDs
   */
  public void addConfiguredXMLCatalog (@Nonnull final XMLCatalog aXmlCatalog)
  {
    m_aXmlCatalog.addConfiguredXMLCatalog (aXmlCatalog);
    log ("Added XMLCatalog " + aXmlCatalog, Project.MSG_DEBUG);
  }

  /**
   * Get the {@link EntityResolver} to be used.
   *
   * @return Never <code>null</code>.
   */
  @Nonnull
  @OverrideOnDemand
  protected EntityResolver getEntityResolver ()
  {
    return m_aXmlCatalog;
  }

  /**
   * Get the {@link URIResolver} to be used.
   *
   * @return Never <code>null</code>.
   */
  @Nonnull
  @OverrideOnDemand
  protected URIResolver getURIResolver ()
  {
    return m_aXmlCatalog;
  }

  @Override
  public void init () throws BuildException
  {
    super.init ();
    m_aXmlCatalog.setProject (getProject ());
  }

  private static final File NULL_FILE_PLACEHOLDER = new File ("/dummy_NULL");

  private static File _getKeyFile (final File f)
  {
    return f != null ? f : NULL_FILE_PLACEHOLDER;
  }

  private void _buildError (@Nonnull final String sMsg)
  {
    _buildError (sMsg, null);
  }

  private void _buildError (@Nonnull final String sMsg, @Nullable final Throwable t)
  {
    if (m_bFailOnError)
      throw new BuildException (sMsg, t);
    log (sMsg, t, Project.MSG_ERR);
  }

  private void _performValidation (@Nonnull final ISchematronResource aSch,
                                   @Nonnull final ICommonsList <ResourceCollection> aResCollections,
                                   @Nullable final File aSVRLDirectory,
                                   final boolean bExpectSuccess) throws BuildException
  {
    // Resolve resourceCollections - pain in the ass
    final ICommonsMap <File, DirectoryData> aFiles = new CommonsHashMap <> ();
    for (final ResourceCollection aResCollection : aResCollections)
    {
      if (!aResCollection.isFilesystemOnly ())
        _buildError ("Only FileSystem resources are supported.");
      else
        for (final Resource aRes : aResCollection)
        {
          if (!aRes.isExists ())
          {
            _buildError ("Could not find resource " + aRes.toLongString () + " to copy.");
            continue;
          }

          File baseDir = NULL_FILE_PLACEHOLDER;
          String name = aRes.getName ();
          final FileProvider fp = aRes.as (FileProvider.class);
          if (fp != null)
          {
            final FileResource fr = ResourceUtils.asFileResource (fp);
            baseDir = _getKeyFile (fr.getBaseDir ());
            if (baseDir == NULL_FILE_PLACEHOLDER)
              name = fr.getFile ().getAbsolutePath ();
          }

          if ((aRes.isDirectory () || fp != null) && name != null)
          {
            final DirectoryData aBaseDir = aFiles.computeIfAbsent (_getKeyFile (baseDir), k -> new DirectoryData (k));
            if (aRes.isDirectory ())
              aBaseDir.addDir (name);
            else
              aBaseDir.addFile (name);
          }
          else
            _buildError ("Could not resolve resource " + aRes.toLongString () + " to a file.");
        }
    }

    for (final DirectoryData aBaseDir : aFiles.values ())
    {
      log ("Scanning directory " + aBaseDir.getBaseDir () + " for XMLs to be Schematron validated", Project.MSG_DEBUG);

      final ICommonsList <String> aIncludes = new CommonsArrayList <> ();
      aIncludes.addAll (aBaseDir.getFiles ());
      for (final String sFile : aBaseDir.getDirs ())
        aIncludes.add (sFile + "/**");

      final DirectoryScanner aScanner = new DirectoryScanner ();
      aScanner.setBasedir (aBaseDir.getBaseDir ());
      if (aIncludes.isNotEmpty ())
        aScanner.setIncludes (aIncludes.toArray (new String [0]));
      aScanner.setCaseSensitive (true);
      aScanner.scan ();

      final String [] aXMLFilenames = aScanner.getIncludedFiles ();
      if (aXMLFilenames != null)
      {
        for (final String sXMLFilename : aXMLFilenames)
        {
          final File aXMLFile = new File (aBaseDir.getBaseDir (), sXMLFilename);

          // Validate XML file
          log ("Validating XML file '" +
               aXMLFile.getPath () +
               "' against Schematron rules from '" +
               m_aSchematronFile.getName () +
               "' expecting " +
               (bExpectSuccess ? "success" : "failure"),
               Project.MSG_INFO);
          try
          {
            final SchematronOutputType aSOT = aSch.applySchematronValidationToSVRL (TransformSourceFactory.create (aXMLFile));

            if (aSVRLDirectory != null)
            {
              // Save SVRL
              final File aSVRLFile = new File (aSVRLDirectory, sXMLFilename + ".svrl");
              if (!aSVRLFile.getParentFile ().mkdirs ())
                log ("Failed to create parent directory of '" + aSVRLFile.getAbsolutePath () + "'!", Project.MSG_ERR);

              if (new SVRLMarshaller ().write (aSOT, aSVRLFile).isSuccess ())
                log ("Successfully saved SVRL file '" + aSVRLFile.getPath () + "'", Project.MSG_INFO);
              else
                log ("Error saving SVRL file '" + aSVRLFile.getPath () + "'", Project.MSG_ERR);
            }

            if (false)
              System.out.println (new SVRLMarshaller ().getAsString (aSOT));

            final ICommonsList <AbstractSVRLMessage> aMessages = SVRLHelper.getAllFailedAssertionsAndSuccessfulReports (aSOT);
            final int nErrorMessages = aMessages.getCount (x -> x.getFlag ().isError ());
            final int nWarningMessages = aMessages.size () - nErrorMessages;
            final String sErrors = nErrorMessages + " Schematron error" + (nErrorMessages == 1 ? "" : "s");
            final String sWarnings = nWarningMessages + " Schematron warning" + (nWarningMessages == 1 ? "" : "s");

            if (bExpectSuccess)
            {
              // No failed assertions expected
              if (nErrorMessages > 0)
              {
                final String sMessage = sErrors +
                                        (nWarningMessages > 0 ? " and " + sWarnings : "") +
                                        " for XML file '" +
                                        aXMLFile.getPath () +
                                        "'";
                log (sMessage, Project.MSG_ERR);

                for (final AbstractSVRLMessage aMsg : aMessages)
                {
                  log (ErrorTextProvider.DEFAULT.getErrorText (aMsg.getAsResourceError (aXMLFile.getPath ()),
                                                               Locale.US),
                       aMsg.getFlag ().isError () ? Project.MSG_ERR : Project.MSG_WARN);
                }
                _buildError (sMessage);
                continue;
              }

              // Success as expected
              log ("XML file '" +
                   aXMLFile.getPath () +
                   "' was validated against Schematron '" +
                   aSch.getResource ().getPath () +
                   "' and matches the rules" +
                   (nWarningMessages > 0 ? " - only " + sWarnings + " are contained" : ""),
                   Project.MSG_INFO);
            }
            else
            {
              // At least one failed assertions expected
              if (nErrorMessages == 0)
              {
                String sMessage = "No Schematron errors for erroneous XML file '" + aXMLFile.getPath () + "'";
                if (nWarningMessages > 0)
                  sMessage += " - only " + sWarnings + " are contained";

                log (sMessage, Project.MSG_ERR);
                _buildError (sMessage);
                continue;
              }

              // Success as expected
              log ("XML file '" +
                   aXMLFile.getPath () +
                   "' was validated against Schematron '" +
                   aSch.getResource ().getPath () +
                   "' " +
                   sErrors +
                   (nWarningMessages > 0 ? " and " + sWarnings : "") +
                   " were found (as expected)",
                   Project.MSG_INFO);
            }
          }
          catch (final BuildException up)
          {
            throw up;
          }
          catch (final Exception ex)
          {
            final String sMessage = "Exception validating XML '" +
                                    aXMLFile.getPath () +
                                    "' against Schematron rules from '" +
                                    m_aSchematronFile.getName () +
                                    "'. Technical details: " +
                                    ex.getClass ().getSimpleName () +
                                    " - " +
                                    ex.getMessage ();
            log (sMessage, ex, Project.MSG_DEBUG);
            _buildError (sMessage, ex);
            continue;
          }
        }
      }
    }
  }

  @Override
  public void execute () throws BuildException
  {
    boolean bCanRun = false;
    if (m_aSchematronFile == null)
      _buildError ("No Schematron file specified!");
    else
      if (m_aSchematronFile.exists () && !m_aSchematronFile.isFile ())
        _buildError ("The specified Schematron file " + m_aSchematronFile + " is not a file!");
      else
        if (m_eSchematronProcessingEngine == null)
          _buildError ("An invalid Schematron processing instance is specified! Only one of the following values is allowed: " +
                       StringHelper.getImplodedMapped (", ", ESchematronMode.values (), x -> "'" + x.getID () + "'"));
        else
          if (m_aResCollections.isEmpty ())
            _buildError ("No XML resources to be validated specified! Add e.g. a <fileset> element.");
          else
            if (m_aSvrlDirectory != null && !m_aSvrlDirectory.exists () && !m_aSvrlDirectory.mkdirs ())
              _buildError ("Failed to create the SVRL directory " + m_aSvrlDirectory);
            else
              bCanRun = true;

    if (bCanRun)
    {
      // 1. Parse Schematron file
      final Locale aDisplayLocale = Locale.US;
      ISchematronResource aSch = null;
      IErrorList aSCHErrors = null;
      switch (m_eSchematronProcessingEngine)
      {
        case PURE:
        {
          // pure
          final CollectingPSErrorHandler aErrorHdl = new CollectingPSErrorHandler ();
          final SchematronResourcePure aRealSCH = new SchematronResourcePure (new FileSystemResource (m_aSchematronFile));
          aRealSCH.setPhase (m_sPhaseName);
          aRealSCH.setErrorHandler (aErrorHdl);
          aRealSCH.setEntityResolver (getEntityResolver ());
          aRealSCH.validateCompletely ();

          aSch = aRealSCH;
          aSCHErrors = aErrorHdl.getAllErrors ();
          break;
        }
        case SCHEMATRON:
        {
          // SCH
          final CollectingTransformErrorListener aErrorHdl = new CollectingTransformErrorListener ();
          final SchematronResourceSCH aRealSCH = new SchematronResourceSCH (new FileSystemResource (m_aSchematronFile));
          aRealSCH.setPhase (m_sPhaseName);
          aRealSCH.setLanguageCode (m_sLanguageCode);
          aRealSCH.setErrorListener (aErrorHdl);
          aRealSCH.setURIResolver (getURIResolver ());
          aRealSCH.setEntityResolver (getEntityResolver ());
          aRealSCH.isValidSchematron ();

          aSch = aRealSCH;
          aSCHErrors = aErrorHdl.getErrorList ();
          break;
        }
        case XSLT:
        {
          // SCH
          final CollectingTransformErrorListener aErrorHdl = new CollectingTransformErrorListener ();
          final SchematronResourceXSLT aRealSCH = new SchematronResourceXSLT (new FileSystemResource (m_aSchematronFile));
          // phase and language are ignored because this was decided when the
          // XSLT
          // was created
          aRealSCH.setErrorListener (aErrorHdl);
          aRealSCH.setURIResolver (getURIResolver ());
          aRealSCH.setEntityResolver (getEntityResolver ());
          aRealSCH.isValidSchematron ();

          aSch = aRealSCH;
          aSCHErrors = aErrorHdl.getErrorList ();
          break;
        }
        default:
          _buildError ("No handler for processing engine '" + m_eSchematronProcessingEngine + "'");
          break;
      }
      if (aSCHErrors != null)
      {
        // Error validating the Schematrons!!
        boolean bAnyParsingError = false;
        for (final IError aError : aSCHErrors)
          if (aError.getErrorLevel ().isGE (EErrorLevel.ERROR))
          {
            log ("Error in Schematron: " + aError.getAsString (aDisplayLocale), Project.MSG_ERR);
            bAnyParsingError = true;
          }
          else
            if (aError.getErrorLevel ().isGE (EErrorLevel.WARN))
              log ("Warning in Schematron: " + aError.getAsString (aDisplayLocale), Project.MSG_WARN);

        if (bAnyParsingError)
          _buildError ("The provided Schematron file contains errors. See log for details.");
        else
        {
          log ("Successfully parsed Schematron file '" + m_aSchematronFile.getPath () + "'", Project.MSG_INFO);

          // 2. for all XML files that match the pattern
          _performValidation (aSch, m_aResCollections, m_aSvrlDirectory, m_bExpectSuccess);
        }
      }
    }
  }
}
