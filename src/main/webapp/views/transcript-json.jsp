<%@ page language="java" import="gov.nysenate.openleg.util.serialize.*,gov.nysenate.openleg.model.transcript.*"  pageEncoding="utf-8" contentType="text/plain"%><%


Transcript transcript = (Transcript)request.getAttribute("transcript");

%><%=OriginalApiConverter.doJson(transcript) %>