<!--
  ~  This file is part of ***  M y C o R e  ***
  ~  See http://www.mycore.de/ for details.
  ~
  ~  This program is free software; you can use it, redistribute it
  ~  and / or modify it under the terms of the GNU General Public License
  ~  (GPL) as published by the Free Software Foundation; either version 2
  ~  of the License or (at your option) any later version.
  ~
  ~  This program is distributed in the hope that it will be useful, but
  ~  WITHOUT ANY WARRANTY; without even the implied warranty of
  ~  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~  GNU General Public License for more details.
  ~
  ~  You should have received a copy of the GNU General Public License
  ~  along with this program, in a file called gpl.txt or license.txt.
  ~  If not, write to the Free Software Foundation Inc.,
  ~  59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
  ~
  -->

<!--
  ~  This file is part of ***  M y C o R e  ***
  ~  See http://www.mycore.de/ for details.
  ~
  ~  This program is free software; you can use it, redistribute it
  ~  and / or modify it under the terms of the GNU General Public License
  ~  (GPL) as published by the Free Software Foundation; either version 2
  ~  of the License or (at your option) any later version.
  ~
  ~  This program is distributed in the hope that it will be useful, but
  ~  WITHOUT ANY WARRANTY; without even the implied warranty of
  ~  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~  GNU General Public License for more details.
  ~
  ~  You should have received a copy of the GNU General Public License
  ~  along with this program, in a file called gpl.txt or license.txt.
  ~  If not, write to the Free Software Foundation Inc.,
  ~  59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
  ~
  -->

<xsl:stylesheet version="1.0"
                xmlns:mets="http://www.loc.gov/METS/"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
                xmlns:ranges="xalan://de.vzg.metsprinter.Ranges"
                xmlns:utils="xalan://de.vzg.metsprinter.Utils"
                exclude-result-prefixes="utils i18n ranges">

  <xsl:param name="derivateID" />
  <xsl:param name="objectID" />
  <xsl:param name="ranges"
             select="concat('1-', string(count(/mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div/mets:div)))" />

  <xsl:param name="headerBackgroundColor" select="'transparent'" />
  <xsl:param name="bodyBackgroundColor" select="'transparent'" />
  <xsl:param name="footerBackgroundColor" select="'transparent'" />

  <xsl:key name="file" match="mets:file" use="@ID" />
  <xsl:variable name="pages" select="ranges:new(string($ranges))" />
  <xsl:variable name="pagesInRange"
                select="/mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div/mets:div[ranges:isIn($pages,number(position()))]" />
  <xsl:key use="@xlink:from" name="smLinkFrom"
           match="mets:smLink[@xlink:to=/mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div/mets:div[ranges:isIn($pages,number(position()))]/@ID]" />

  <xsl:template match="/">
    <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="/mets:mets">

    <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format" id="root" font-family="Frutiger" color="#58585a">
      <xsl:comment>
        <xsl:value-of select="$ranges" />
      </xsl:comment>
      <fo:layout-master-set>
        <fo:simple-page-master master-name="titleLayout" page-height="29.7cm" page-width="21cm" margin-top="1cm"
                               margin-bottom="1cm" margin-left="2cm" margin-right="2cm">
          <fo:region-body background-color="{$bodyBackgroundColor}" margin-top="6cm" margin-bottom="9.2cm" />
          <fo:region-before background-color="{$headerBackgroundColor}" extent="6cm" region-name="header-title" />
          <fo:region-after background-color="{$footerBackgroundColor}" extent="9.2cm" region-name="footer-title" />
        </fo:simple-page-master>
        <fo:simple-page-master master-name="pageLayout" page-height="29.7cm" page-width="21cm" margin-top="1cm"
                               margin-bottom="1cm" margin-left="16mm" margin-right="16mm">
          <fo:region-body background-color="{$bodyBackgroundColor}" margin-top="1cm" margin-bottom="2cm" />
          <fo:region-before background-color="{$headerBackgroundColor}" extent="1cm" region-name="header-page" />
          <fo:region-after background-color="{$footerBackgroundColor}" extent="2cm" region-name="footer-page" />
        </fo:simple-page-master>
        <fo:page-sequence-master master-name="singlePage">
          <fo:single-page-master-reference master-reference="pageLayout" />
        </fo:page-sequence-master>
      </fo:layout-master-set>
      <fo:declarations>

      </fo:declarations>

      <fo:bookmark-tree>
        <xsl:apply-templates select="mets:structMap[@TYPE='LOGICAL']/mets:div" mode="bookmark" />
      </fo:bookmark-tree>

      <xsl:apply-templates select="." mode="physicalPage" />
    </fo:root>
  </xsl:template>

  <xsl:template match="mets:div" mode="bookmark">
    <xsl:variable name="logID" select="@ID" />
    <xsl:variable name="dest">
      <xsl:apply-templates select="." mode="destID" />
    </xsl:variable>
    <xsl:if test="string-length($dest)&gt;0">
      <fo:bookmark>
        <xsl:attribute name="internal-destination">
          <xsl:value-of select="$dest" />
        </xsl:attribute>
        <fo:bookmark-title>
          <xsl:choose>
            <xsl:when test="@LABEL">
              <xsl:value-of select="@LABEL" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="i18n:translate(concat('component.mets.dfgStructureSet.', @TYPE))" />
            </xsl:otherwise>
          </xsl:choose>
        </fo:bookmark-title>
        <xsl:for-each select="mets:div">
          <xsl:apply-templates select="." mode="bookmark" />
        </xsl:for-each>
      </fo:bookmark>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mets:div" mode="destID">
    <xsl:variable name="logID" select="@ID" />
    <xsl:choose>
      <xsl:when test="key('smLinkFrom', $logID)">
        <xsl:value-of select="key('smLinkFrom', $logID)/@xlink:to" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="mets:div" mode="destID" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:variable name="picturePageWidth" select="'17.8cm'" />
  <!-- always 2mm less than calculated or extra page -->
  <xsl:variable name="picturePageHeight" select="'24.4cm'" />
  <xsl:variable name="footerTableColor" select="'transparent'" />

  <xsl:template match="mets:mets" mode="physicalPage">
    <xsl:for-each select="$pagesInRange">

      <xsl:variable name="physID" select="@ID" />
      <xsl:variable name="order" select="count(/mets:mets/mets:structMap[@TYPE='PHYSICAL']//mets:div[@ID=$physID]/preceding-sibling::mets:div)+1" />

      <fo:page-sequence master-reference="singlePage">
        <!-- print footer -->
        <fo:static-content flow-name="footer-page">
          <xsl:apply-templates select="." mode="displayFooter">
            <xsl:with-param name="order" select="$order" />
          </xsl:apply-templates>
        </fo:static-content>

        <!-- print pictures -->
        <fo:flow flow-name="xsl-region-body">
          <xsl:apply-templates select="." mode="displayPage" />
          <xsl:if test="position()=last()">
            <fo:block id="last-page" background-color="blue"></fo:block>
          </xsl:if>
        </fo:flow>
      </fo:page-sequence>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="mets:div" mode="displayFooter">
    <xsl:param name="withPageNumber" select="'true'" />
    <xsl:param name="order" />
    <xsl:variable name="urn" select="@CONTENTIDS" />
    <xsl:variable name="orderLabel" select="@ORDERLABEL" />
    <fo:table width="{$picturePageWidth}" table-layout="fixed" background-color="{$footerTableColor}">
      <fo:table-column column-width="3cm" />
      <fo:table-column column-width="11.8cm" />
      <fo:table-column column-width="3cm" />
      <fo:table-body>
        <fo:table-row keep-together.within-column="always" border-bottom="solid #b1b3b4">
          <fo:table-cell>
            <fo:block />
          </fo:table-cell>
          <fo:table-cell display-align="center">
            <fo:block font-size="10pt" text-align="center" display-align="center">
              <xsl:if test="$withPageNumber='true'">
                Seite
                <fo:page-number />
                von
                <fo:page-number-citation ref-id="last-page" />
              </xsl:if>
            </fo:block>
          </fo:table-cell>
          <fo:table-cell>
            <fo:block font-size="10pt">
              <xsl:variable name="page">
                <xsl:if test="string-length($orderLabel) &gt; 0">
                  <xsl:value-of select="concat(' - ', $orderLabel)" />
                </xsl:if>
              </xsl:variable>
              <xsl:value-of select="concat('Bild: ',$order, $page)" />
            </fo:block>
          </fo:table-cell>
        </fo:table-row>
      </fo:table-body>
    </fo:table>
  </xsl:template>

  <xsl:template match="mets:div" mode="displayPage">
    <xsl:variable name="iviewGroup" select="key('file', mets:fptr/@FILEID)[../@USE='IVIEW']" />
    <xsl:variable name="masterGroup" select="key('file', mets:fptr/@FILEID)[../@USE='MASTER']" />
    <xsl:variable name="physID" select="@ID" />
    <xsl:choose>
      <xsl:when test="count($iviewGroup)&gt;=count($masterGroup)">
        <xsl:apply-templates select="$iviewGroup" mode="displayPage">
          <xsl:with-param name="physID" select="$physID" />
        </xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="$masterGroup" mode="displayPage">
          <xsl:with-param name="physID" select="$physID" />
        </xsl:apply-templates>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mets:file" mode="displayPage">
    <xsl:param name="physID" />
    <fo:block break-before="page">
      <xsl:attribute name="id">
        <xsl:value-of select="$physID" />
      </xsl:attribute>
      <fo:external-graphic src="url('{utils:getIFSPath($derivateID, mets:FLocat/@xlink:href)}')"
                           content-width="scale-to-fit"
                           content-height="scale-to-fit"
                           scaling="uniform" height="{$picturePageHeight}" width="{$picturePageWidth}" />
    </fo:block>
  </xsl:template>

</xsl:stylesheet>
