/**
 * Copyright (C) 2017-2019 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.helger.commons.io.resource.FileSystemResource;
import com.helger.schematron.pure.binding.xpath.PSXPathQueryBinding;
import com.helger.schematron.pure.exchange.PSReader;
import com.helger.schematron.pure.exchange.PSWriter;
import com.helger.schematron.pure.exchange.PSWriterSettings;
import com.helger.schematron.pure.exchange.SchematronReadException;
import com.helger.schematron.pure.model.PSSchema;
import com.helger.schematron.pure.preprocess.PSPreprocessor;
import com.helger.schematron.pure.preprocess.SchematronPreprocessException;
import com.helger.xml.serialize.write.XMLWriterSettings;

/**
 * ANT task to perform Schematron preprocessing. It converts an existing schema
 * to the minimal syntax (by default) but allows for a certain degree of
 * customization by keeping certain elements in the resulting schema. The actual
 * query binding is used, so that report test expressions can be converted to
 * assertions, and to replace the content of &lt;param&gt; elements into actual
 * values.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public class SchematronPreprocess extends Task
{
  /**
   * The Schematron source file to be pre-processed.
   */
  private File m_aSrcFile;

  /**
   * The Schematron destination file to be written.
   */
  private File m_aDstFile;

  /**
   * <code>true</code> if &lt;title&gt;-elements should be kept.
   */
  private boolean m_bKeepTitles = PSPreprocessor.DEFAULT_KEEP_TITLES;

  /**
   * <code>true</code> if &lt;diagnostic&gt;-elements should be kept.
   */
  private boolean m_bKeepDiagnostics = PSPreprocessor.DEFAULT_KEEP_DIAGNOSTICS;

  /**
   * Should &lt;report&gt;-elements be kept or should they be converted to
   * &lt;assert&gt;-elements?
   */
  private boolean m_bKeepReports = PSPreprocessor.DEFAULT_KEEP_REPORTS;

  /**
   * Should &lt;pattern&gt;-elements without a single rule be kept or deleted?
   */
  private boolean m_bKeepEmptyPatterns = PSPreprocessor.DEFAULT_KEEP_EMPTY_PATTERNS;

  /**
   * <code>true</code> if the build should fail if any error occurs. Defaults to
   * <code>true</code>.
   */
  private boolean m_bFailOnError = true;

  public SchematronPreprocess ()
  {}

  public void setSrcFile (@Nonnull final File aFile)
  {
    m_aSrcFile = aFile;
    if (!m_aSrcFile.isAbsolute ())
      m_aSrcFile = new File (getProject ().getBaseDir (), aFile.getPath ());
    log ("Using source Schematron file '" + m_aSrcFile + "'", Project.MSG_DEBUG);
  }

  public void setDstFile (@Nonnull final File aFile)
  {
    m_aDstFile = aFile;
    if (!m_aDstFile.isAbsolute ())
      m_aDstFile = new File (getProject ().getBaseDir (), aFile.getPath ());
    log ("Using destination Schematron file '" + m_aDstFile + "'", Project.MSG_DEBUG);
  }

  public void setKeepTitles (final boolean bKeepTitles)
  {
    m_bKeepTitles = bKeepTitles;
    log (bKeepTitles ? "Keeping <title>-elements." : "Removing <title>-elements.", Project.MSG_DEBUG);
  }

  public void setKeepDiagnostics (final boolean bKeepDiagnostics)
  {
    m_bKeepDiagnostics = bKeepDiagnostics;
    log (bKeepDiagnostics ? "Keeping <diagnostic>-elements." : "Removing <diagnostic>-elements.", Project.MSG_DEBUG);
  }

  public void setKeepReports (final boolean bKeepReports)
  {
    m_bKeepReports = bKeepReports;
    log (bKeepReports ? "Keeping <report>-elements." : "Changing to <assert>-elements.", Project.MSG_DEBUG);
  }

  public void setKeepEmptyPatterns (final boolean bKeepEmptyPatterns)
  {
    m_bKeepEmptyPatterns = bKeepEmptyPatterns;
    log (bKeepEmptyPatterns ? "Keeping <pattern>-elements without rules."
                            : "Deleting <pattern>-elements without rules.",
         Project.MSG_DEBUG);
  }

  public void setFailOnError (final boolean bFailOnError)
  {
    m_bFailOnError = bFailOnError;

    log (bFailOnError ? "Will fail on error" : "Will not fail on error", Project.MSG_DEBUG);
  }

  private void _error (@Nonnull final String sMsg)
  {
    _error (sMsg, null);
  }

  private void _error (@Nonnull final String sMsg, @Nullable final Throwable t)
  {
    if (m_bFailOnError)
      throw new BuildException (sMsg, t);
    log (sMsg, t, Project.MSG_ERR);
  }

  @Override
  public void execute () throws BuildException
  {
    boolean bCanRun = false;
    if (m_aSrcFile == null)
      _error ("No source Schematron file specified!");
    else
      if (m_aSrcFile.exists () && !m_aSrcFile.isFile ())
        _error ("The specified source Schematron file " + m_aSrcFile + " is not a file!");
      else
        if (m_aDstFile == null)
          _error ("No destination Schematron file specified!");
        else
          if (m_aDstFile.exists () && !m_aDstFile.isFile ())
            _error ("The specified destination Schematron file " + m_aDstFile + " is not a file!");
          else
            bCanRun = true;

    if (bCanRun)
      try
      {
        // Read source
        final PSSchema aSchema = new PSReader (new FileSystemResource (m_aSrcFile)).readSchema ();

        // Setup preprocessor
        final PSPreprocessor aPreprocessor = new PSPreprocessor (PSXPathQueryBinding.getInstance ());
        aPreprocessor.setKeepTitles (m_bKeepTitles);
        aPreprocessor.setKeepDiagnostics (m_bKeepDiagnostics);
        aPreprocessor.setKeepReports (m_bKeepReports);
        aPreprocessor.setKeepEmptyPatterns (m_bKeepEmptyPatterns);
        aPreprocessor.setKeepEmptySchema (true);

        // Main pre-processing
        final PSSchema aPreprocessedSchema = aPreprocessor.getAsPreprocessedSchema (aSchema);

        // Write the result file
        new PSWriter (new PSWriterSettings ().setXMLWriterSettings (new XMLWriterSettings ())).writeToFile (aPreprocessedSchema,
                                                                                                            m_aDstFile);
        log ("Successfully pre-processed Schematron " + m_aSrcFile + " to " + m_aDstFile);
      }
      catch (final SchematronReadException | SchematronPreprocessException ex)
      {
        _error ("Error processing Schemtron " + m_aSrcFile.getAbsolutePath (), ex);
      }
  }
}
