<%@ page language="java" import="java.util.*,java.text.*,gov.nysenate.openleg.*,gov.nysenate.openleg.model.bill.*,gov.nysenate.openleg.util.serialize.*" pageEncoding="utf-8" contentType="text/xml"%><%

Collection<Bill> bills = (Collection<Bill>)request.getAttribute("bills");

%><%=BillRenderer.renderBills(bills,false)%>