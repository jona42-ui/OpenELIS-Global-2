<%@ page language="java"
         contentType="text/html; charset=utf-8"
         import="java.util.List,
         		us.mn.state.health.lims.common.action.IActionConstants,
         		us.mn.state.health.lims.common.util.IdValuePair,
         		us.mn.state.health.lims.common.util.StringUtil,
         		us.mn.state.health.lims.common.util.Versioning" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="app" uri="/tags/labdev-view" %>
<%@ taglib prefix="ajax" uri="/tags/ajaxtags" %>
<%--
  ~ The contents of this file are subject to the Mozilla Public License
  ~ Version 1.1 (the "License"); you may not use this file except in
  ~ compliance with the License. You may obtain a copy of the License at
  ~ http://www.mozilla.org/MPL/
  ~
  ~ Software distributed under the License is distributed on an "AS IS"
  ~ basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
  ~ License for the specific language governing rights and limitations under
  ~ the License.
  ~
  ~ The Original Code is OpenELIS code.
  ~
  ~ Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
  --%>

<script type="text/javascript" src="scripts/ajaxCalls.js?ver=<%= Versioning.getBuildNumber() %>"></script>
<script type="text/javascript" src="scripts/jquery-ui.js?ver=<%= Versioning.getBuildNumber() %>"></script>

 <c:set var="sampleTypeList" value="${form.sampleTypeList}" />

<%!
    String basePath = "";
    int testCount = 0;
    int columnCount = 0;
    int columns = 4;
%>

<%
    basePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/";
    columnCount = 0;
    testCount = 0;
%>

<link rel="stylesheet" media="screen" type="text/css"
      href="<%=basePath%>css/jquery_ui/jquery.ui.theme.css?ver=<%= Versioning.getBuildNumber() %>"/>

<script type="text/javascript">
    if (!$jq) {
        var $jq = jQuery.noConflict();
    }

    $jq(document).ready( function(){
        $jq(".sortable").sortable({
            stop: function( ) {makeDirty();}
        });
    });

    function makeDirty(){
        function formWarning(){
            return "<spring:message code="banner.menu.dataLossWarning"/>";
        }
        window.onbeforeunload = formWarning;
    }

    function submitAction(target) {
        var form = window.document.forms[0];
        form.action = target;
        form.submit();
    }


    function confirmValues() {
        $jq("#editButtons").hide();
        $jq("#confirmationButtons").show();
        $jq("#editMessage").hide();
        $jq("#action").text('<%=StringUtil.getContextualMessageForKey("label.confirmation")%>');

        $jq(".sortable").sortable("disable");
    }

    function rejectConfirmation() {
        $jq("#editButtons").show();
        $jq("#confirmationButtons").hide();
        $jq("#editMessage").show();
        $jq("#action").text('<%=StringUtil.getContextualMessageForKey("label.button.edit")%>');

        $jq(".sortable").sortable("enable");
    }

    function buildJSONList(){
        var sortOrder = 0;
        var jsonObj = {};
        jsonObj.sampleTypes = [];

        $jq("li.sortItem").each(function(){
            jsonBlob = {};
            jsonBlob.id = $jq(this).val();
            jsonBlob.sortOrder = sortOrder++;
            jsonObj.sampleTypes[sortOrder - 1] = jsonBlob;
        });

        $jq("#jsonChangeList").val(JSON.stringify(jsonObj));
    }
    function savePage() {
        buildJSONList();
        window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
        var form = window.document.forms[0];
        form.action = "SampleTypeOrder.do";
        form.submit();
    }
</script>

<style>
table{
  width: 80%;
}
td {
  width: 25%;
}
</style>

<form:form name="${form.formName}" 
				   action="${form.formAction}" 
				   modelAttribute="form" 
				   onSubmit="return submitForm(this);" 
				   method="${form.formMethod}"
				   id="mainForm">

    <form:hidden path="jsonChangeList" id="jsonChangeList"/>

    <input type="button" value='<%= StringUtil.getContextualMessageForKey("banner.menu.administration") %>'
           onclick="submitAction('MasterListsPage.do');"
           class="textButton"/>&rarr;
    <input type="button" value='<%= StringUtil.getContextualMessageForKey("configuration.test.management") %>'
           onclick="submitAction('TestManagementConfigMenu.do');"
           class="textButton"/>&rarr;
    <input type="button" value='<%= StringUtil.getContextualMessageForKey("configuration.sampleType.manage") %>'
           onclick="submitAction('SampleTypeManagement.do');"
           class="textButton"/>&rarr;

<%=StringUtil.getContextualMessageForKey( "configuration.sampleType.order" )%>

<%    List sampleTypeList = (List) pageContext.getAttribute("sampleTypeList"); %>
<br><br>

<div id="editDiv" >
    <h1 id="action"><spring:message code="label.button.edit"/></h1>

    <div id="editMessage" >
        <h3><spring:message code="configuration.sampleType.order.explain"/> </h3>
        <spring:message code="configuration.sampleType.order.explain.limits" /><br/><br/>
    </div>

    <UL class="sortable" style="width:250px">
        <% for(int i = 0; i < sampleTypeList.size(); i++){
            IdValuePair sampleType = (IdValuePair)sampleTypeList.get(i);
        %>
        <LI class="ui-state-default_oe sortItem" value='<%=sampleType.getId() %>' ><span class="ui-icon ui-icon-arrowthick-2-n-s" ></span><%=sampleType.getValue() %></LI>
        <% } %>

    </UL>

    <div style="text-align: center" id="editButtons">
        <input type="button" value='<%=StringUtil.getContextualMessageForKey("label.button.next")%>'
               onclick="confirmValues();"/>
        <input type="button" value='<%=StringUtil.getContextualMessageForKey("label.button.previous")%>'
               onclick='submitAction("SampleTypeManagement.do")'/>
    </div>
    <div style="text-align: center; display: none;" id="confirmationButtons">
        <input type="button" value='<%=StringUtil.getContextualMessageForKey("label.button.accept")%>'
               onclick="savePage();"/>
        <input type="button" value='<%=StringUtil.getContextualMessageForKey("label.button.reject")%>'
               onclick='rejectConfirmation();'/>
    </div>
</div>
</form:form>


