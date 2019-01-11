/**
 * Copyright (C) 2014-2019 Philip Helger (www.helger.com)
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
package com.helger.schematron.pure.model;

import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.schematron.CSchematron;
import com.helger.schematron.CSchematronXML;
import com.helger.schematron.pure.errorhandler.IPSErrorHandler;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

/**
 * A single Schematron pattern-element.<br>
 * A structure, simple or complex. A set of rules giving constraints that are in
 * some way related. The id attribute provides a unique name for the pattern and
 * is required for abstract patterns.<br>
 * The title and p elements allow rich documentation.<br>
 * The icon, see and fpi attributes allow rich interfaces and documentation.<br>
 * When a pattern element has the attribute abstract with a value true, then the
 * pattern defines an abstract pattern. An abstract pattern shall not have a
 * is-a attribute and shall have an id attribute.<br>
 * Abstract patterns allow a common definition mechanism for structures which
 * use different names and paths, but which are at heart the same. For example,
 * there are different table markup languages, but they all can be in large part
 * represented as an abstract pattern where a table contains rows and rows
 * contain entries, as defined in the following example using the default query
 * language binding:<br>
 *
 * <pre>
 *     &lt;sch:pattern abstract="true" id="table"&gt;
 *         &lt;sch:rule context="$table"&gt;
 *             &lt;sch:assert test="$row"&gt;
 *             The element &lt;sch:name/&gt; is a table. Tables contain rows.
 *             &lt;/sch:assert&gt;
 *         &lt;/sch:rule&gt;
 *         &lt;sch:rule context="$row"&gt;
 *             &lt;sch:assert test="$entry"&gt;
 *             The element &lt;sch:name/&gt; is a table row. Rows contain entries.
 *             &lt;/sch:assert&gt;
 *         &lt;/sch:rule&gt;
 *     &lt;/sch:pattern&gt;
 * </pre>
 *
 * When a pattern element has the attribute is-a with a value specifying the
 * name of an abstract pattern, then the pattern is an instance of an abstract
 * pattern. Such a pattern shall not contain any rule elements, but shall have
 * param elements for all parameters used in the abstract pattern.<br>
 * The following example uses the abstract pattern for tables given above to
 * create three patterns for tables with different names or structures.<br>
 *
 * <pre>
 *     &lt;sch:pattern is-a="table" id="HTML_Table"&gt;
 *         &lt;sch:param name="table" value="table"/&gt;
 *         &lt;sch:param name="row"   value="tr"/&gt;
 *         &lt;sch:param name="entry" value="td|th"/&gt;
 *     &lt;/sch:pattern&gt;
 *     &lt;sch:pattern is-a="table" id="CALS_Table"&gt;
 *         &lt;sch:param name="table" value="table"/&gt;
 *         &lt;sch:param name="row"   value=".//row"/&gt;
 *         &lt;sch:param name="entry" value="cell"/&gt;
 *     &lt;/sch:pattern&gt;
 *     &lt;sch:pattern is-a="table" id="calendar"&gt;
 *         &lt;sch:param name="table" value="calendar/year"/&gt;
 *         &lt;sch:param name="row"   value="week"/&gt;
 *         &lt;sch:param name="entry" value="day"/&gt;
 *     &lt;/sch:pattern&gt;
 * </pre>
 *
 * When creating an instance of an abstract pattern, the parameter values
 * supplied by the param element replace the parameter references used in the
 * abstract patterns. The examples above use the default query language binding
 * in which the character $ is used as the delimiter for parameter references.
 * <br>
 * Thus, given the abstract patterns defined earlier in this clause, the
 * patterns defined above are equivalent to the following, with the id elements
 * shown expanded:<br>
 *
 * <pre>
 *     &lt;sch:pattern id="HTML_table"&gt;
 *         &lt;sch:rule context="table"&gt;
 *             &lt;sch:assert test="tr"&gt;
 *             The element table is a table. Tables containing rows.
 *             &lt;/sch:assert&gt;
 *         &lt;/sch:rule&gt;
 *         &lt;sch:rule context="tr"&gt;
 *             &lt;sch:assert test="td|th"&gt;
 *             The element tr is a table row. Rows contain entries.
 *             &lt;/sch:assert&gt;
 *         &lt;/sch:rule&gt;
 *     &lt;/sch:pattern&gt;
 *     &lt;sch:pattern id="CALS_table"&gt;
 *         &lt;sch:rule context="table"&gt;
 *             &lt;sch:assert test=".//row"&gt;
 *             The element table is a table. Tables containing rows.
 *             &lt;/sch:assert&gt;
 *         &lt;/sch:rule&gt;
 *         &lt;sch:rule context=".//row"&gt;
 *             &lt;sch:assert test="cell"&gt;
 *             The element row is a table row. Rows contain entries.
 *             &lt;/sch:assert&gt;
 *         &lt;/sch:rule&gt;
 *     &lt;/sch:pattern&gt;
 *     &lt;sch:pattern id="calendar"&gt;
 *         &lt;sch:rule context="calendar/year"&gt;
 *             &lt;sch:assert test="week"&gt;
 *             The element year is a table. Tables containing rows.
 *             &lt;/sch:assert&gt;
 *         &lt;/sch:rule&gt;
 *         &lt;sch:rule context="week"&gt;
 *             &lt;sch:assert test="day"&gt;
 *             The element week is a table row. Rows contain entries.
 *             &lt;/sch:assert&gt;
 *         &lt;/sch:rule&gt;
 *     &lt;/sch:pattern&gt;
 * </pre>
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class PSPattern implements
                       IPSElement,
                       IPSHasID,
                       IPSHasForeignElements,
                       IPSHasIncludes,
                       IPSHasLets,
                       IPSHasRichGroup
{
  private boolean m_bAbstract = false;
  private String m_sID;
  private String m_sIsA;
  private PSRichGroup m_aRich;
  private final ICommonsList <PSInclude> m_aIncludes = new CommonsArrayList <> ();
  private PSTitle m_aTitle;
  private final ICommonsList <IPSElement> m_aContent = new CommonsArrayList <> ();
  private ICommonsOrderedMap <String, String> m_aForeignAttrs;
  private ICommonsList <IMicroElement> m_aForeignElements;

  public PSPattern ()
  {}

  public boolean isValid (@Nonnull final IPSErrorHandler aErrorHandler)
  {
    // If abstract, an ID must be present
    if (m_bAbstract && StringHelper.hasNoText (m_sID))
    {
      aErrorHandler.error (this, "abstract <pattern> does not have an 'id'");
      return false;
    }
    // is-a may not be present for abstract rules
    if (m_bAbstract && StringHelper.hasText (m_sIsA))
    {
      aErrorHandler.error (this, "abstract <pattern> may not have an 'is-a'");
      return false;
    }
    if (StringHelper.hasNoText (m_sIsA))
    {
      // param only if is-a is set
      for (final IPSElement aContent : m_aContent)
        if (aContent instanceof PSParam)
        {
          aErrorHandler.error (this, "<pattern> without 'is-a' may not contain <param>s");
          return false;
        }
    }
    else
    {
      // rule and let only if is-a is not set
      for (final IPSElement aContent : m_aContent)
      {
        if (aContent instanceof PSRule)
        {
          aErrorHandler.error (this, "<pattern> with 'is-a' may not contain <rule>s");
          return false;
        }
        if (aContent instanceof PSLet)
        {
          aErrorHandler.error (this, "<pattern> with 'is-a' may not contain <let>s");
          return false;
        }
      }
    }

    for (final PSInclude aInclude : m_aIncludes)
      if (!aInclude.isValid (aErrorHandler))
        return false;
    if (m_aTitle != null && !m_aTitle.isValid (aErrorHandler))
      return false;
    for (final IPSElement aContent : m_aContent)
      if (!aContent.isValid (aErrorHandler))
        return false;
    return true;
  }

  public void validateCompletely (@Nonnull final IPSErrorHandler aErrorHandler)
  {
    // If abstract, an ID must be present
    if (m_bAbstract && StringHelper.hasNoText (m_sID))
      aErrorHandler.error (this, "abstract <pattern> does not have an 'id'");
    // is-a may not be present for abstract rules
    if (m_bAbstract && StringHelper.hasText (m_sIsA))
      aErrorHandler.error (this, "abstract <pattern> may not have an 'is-a'");
    if (StringHelper.hasNoText (m_sIsA))
    {
      // param only if is-a is set
      for (final IPSElement aContent : m_aContent)
        if (aContent instanceof PSParam)
          aErrorHandler.error (this, "<pattern> without 'is-a' may not contain <param>s");
    }
    else
    {
      // rule and let only if is-a is not set
      for (final IPSElement aContent : m_aContent)
      {
        if (aContent instanceof PSRule)
          aErrorHandler.error (this, "<pattern> with 'is-a' may not contain <rule>s");
        if (aContent instanceof PSLet)
          aErrorHandler.error (this, "<pattern> with 'is-a' may not contain <let>s");
      }
    }

    for (final PSInclude aInclude : m_aIncludes)
      aInclude.validateCompletely (aErrorHandler);
    if (m_aTitle != null)
      m_aTitle.validateCompletely (aErrorHandler);
    for (final IPSElement aContent : m_aContent)
      aContent.validateCompletely (aErrorHandler);
  }

  public boolean isMinimal ()
  {
    if (m_bAbstract)
      return false;
    if (StringHelper.hasText (m_sIsA))
      return false;
    for (final PSInclude aInclude : m_aIncludes)
      if (!aInclude.isMinimal ())
        return false;
    if (m_aTitle != null && !m_aTitle.isMinimal ())
      return false;
    for (final IPSElement aContent : m_aContent)
      if (!aContent.isMinimal ())
        return false;
    return true;
  }

  public void addForeignElement (@Nonnull final IMicroElement aForeignElement)
  {
    ValueEnforcer.notNull (aForeignElement, "ForeignElement");
    if (aForeignElement.hasParent ())
      throw new IllegalArgumentException ("ForeignElement already has a parent!");
    if (m_aForeignElements == null)
      m_aForeignElements = new CommonsArrayList <> ();
    m_aForeignElements.add (aForeignElement);
  }

  public boolean hasForeignElements ()
  {
    return m_aForeignElements != null && m_aForeignElements.isNotEmpty ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IMicroElement> getAllForeignElements ()
  {
    return new CommonsArrayList <> (m_aForeignElements);
  }

  public void addForeignAttribute (@Nonnull final String sAttrName, @Nonnull final String sAttrValue)
  {
    ValueEnforcer.notNull (sAttrName, "AttrName");
    ValueEnforcer.notNull (sAttrValue, "AttrValue");
    if (m_aForeignAttrs == null)
      m_aForeignAttrs = new CommonsLinkedHashMap <> ();
    m_aForeignAttrs.put (sAttrName, sAttrValue);
  }

  public boolean hasForeignAttributes ()
  {
    return m_aForeignAttrs != null && m_aForeignAttrs.isNotEmpty ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsOrderedMap <String, String> getAllForeignAttributes ()
  {
    return new CommonsLinkedHashMap <> (m_aForeignAttrs);
  }

  public void setAbstract (final boolean bAbstract)
  {
    m_bAbstract = bAbstract;
  }

  public boolean isAbstract ()
  {
    return m_bAbstract;
  }

  public void setID (@Nullable final String sID)
  {
    m_sID = sID;
  }

  @Nullable
  public String getID ()
  {
    return m_sID;
  }

  public void setIsA (@Nullable final String sIsA)
  {
    m_sIsA = sIsA;
  }

  @Nullable
  public String getIsA ()
  {
    return m_sIsA;
  }

  public void setRich (@Nullable final PSRichGroup aRich)
  {
    m_aRich = aRich;
  }

  @Nullable
  public PSRichGroup getRich ()
  {
    return m_aRich;
  }

  public void addInclude (@Nonnull final PSInclude aInclude)
  {
    ValueEnforcer.notNull (aInclude, "Include");
    m_aIncludes.add (aInclude);
  }

  public boolean hasAnyInclude ()
  {
    return m_aIncludes.isNotEmpty ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <PSInclude> getAllIncludes ()
  {
    return m_aIncludes.getClone ();
  }

  public void setTitle (@Nullable final PSTitle aTitle)
  {
    m_aTitle = aTitle;
  }

  @Nullable
  public PSTitle getTitle ()
  {
    return m_aTitle;
  }

  public boolean hasTitle ()
  {
    return m_aTitle != null;
  }

  public void addRule (@Nonnull final PSRule aRule)
  {
    ValueEnforcer.notNull (aRule, "Rule");
    m_aContent.add (aRule);
  }

  @Nullable
  public PSRule getRuleOfID (@Nullable final String sID)
  {
    if (StringHelper.hasText (sID))
      for (final IPSElement aElement : m_aContent)
        if (aElement instanceof PSRule)
        {
          final PSRule aRule = (PSRule) aElement;
          if (sID.equals (aRule.getID ()))
            return aRule;
        }
    return null;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <PSRule> getAllRules ()
  {
    return m_aContent.getAllInstanceOf (PSRule.class);
  }

  @Nonnegative
  public int getRuleCount ()
  {
    return m_aContent.getCount (e -> e instanceof PSRule);
  }

  public void addParam (@Nonnull final PSParam aParam)
  {
    ValueEnforcer.notNull (aParam, "Param");
    m_aContent.add (aParam);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <PSParam> getAllParams ()
  {
    return m_aContent.getAllInstanceOf (PSParam.class);
  }

  public boolean hasAnyParam ()
  {
    return m_aContent.containsAny (e -> e instanceof PSParam);
  }

  public void addP (@Nonnull final PSP aP)
  {
    ValueEnforcer.notNull (aP, "P");
    m_aContent.add (aP);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <PSP> getAllPs ()
  {
    return m_aContent.getAllInstanceOf (PSP.class);
  }

  public void addLet (@Nonnull final PSLet aLet)
  {
    ValueEnforcer.notNull (aLet, "Let");
    m_aContent.add (aLet);
  }

  public boolean hasAnyLet ()
  {
    return m_aContent.containsAny (e -> e instanceof PSLet);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <PSLet> getAllLets ()
  {
    return m_aContent.getAllInstanceOf (PSLet.class);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsOrderedMap <String, String> getAllLetsAsMap ()
  {
    final ICommonsOrderedMap <String, String> ret = new CommonsLinkedHashMap <> ();
    for (final IPSElement aElement : m_aContent)
      if (aElement instanceof PSLet)
      {
        final PSLet aLet = (PSLet) aElement;
        ret.put (aLet.getName (), aLet.getValue ());
      }
    return ret;
  }

  /**
   * @return A list consisting of {@link PSP}, {@link PSLet}, {@link PSRule} and
   *         {@link PSParam} parameters
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IPSElement> getAllContentElements ()
  {
    return m_aContent.getClone ();
  }

  @Nonnull
  public IMicroElement getAsMicroElement ()
  {
    final IMicroElement ret = new MicroElement (CSchematron.NAMESPACE_SCHEMATRON, CSchematronXML.ELEMENT_PATTERN);
    if (m_bAbstract)
      ret.setAttribute (CSchematronXML.ATTR_ABSTRACT, "true");
    ret.setAttribute (CSchematronXML.ATTR_ID, m_sID);
    ret.setAttribute (CSchematronXML.ATTR_IS_A, m_sIsA);
    if (m_aRich != null)
      m_aRich.fillMicroElement (ret);
    if (m_aForeignElements != null)
      for (final IMicroElement aForeignElement : m_aForeignElements)
        ret.appendChild (aForeignElement.getClone ());
    for (final PSInclude aInclude : m_aIncludes)
      ret.appendChild (aInclude.getAsMicroElement ());
    if (m_aTitle != null)
      ret.appendChild (m_aTitle.getAsMicroElement ());
    for (final IPSElement aContent : m_aContent)
      ret.appendChild (aContent.getAsMicroElement ());
    if (m_aForeignAttrs != null)
      for (final Map.Entry <String, String> aEntry : m_aForeignAttrs.entrySet ())
        ret.setAttribute (aEntry.getKey (), aEntry.getValue ());
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("abstract", m_bAbstract)
                                       .appendIfNotNull ("id", m_sID)
                                       .appendIfNotNull ("is-a", m_sIsA)
                                       .appendIfNotNull ("rich", m_aRich)
                                       .appendIf ("includes", m_aIncludes, CollectionHelper::isNotEmpty)
                                       .appendIfNotNull ("title", m_aTitle)
                                       .appendIf ("content", m_aContent, CollectionHelper::isNotEmpty)
                                       .appendIf ("foreignAttrs", m_aForeignAttrs, CollectionHelper::isNotEmpty)
                                       .appendIf ("foreignElements", m_aForeignElements, CollectionHelper::isNotEmpty)
                                       .getToString ();
  }
}
