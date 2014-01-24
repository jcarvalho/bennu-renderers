<!Doctype html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" scope="request" />

<html>
	<head>
		<c:forEach var="head" items="${_CONTEXT_.head}">
			<jsp:include page="${head}" />
		</c:forEach>
		<script type="text/javascript" src="${contextPath}/bankai/js/libs/mustache/mustache.js"></script>
		<script type="text/javascript" src="${contextPath}/bankai/js/libs/jquery/jquery.js"></script>
		<c:forEach var="script" items="${_CONTEXT_.scripts}">
			<jsp:include page="${script}" />
		</c:forEach>
		<script type="text/javascript" src="${contextPath}/bennu-portal/portal.js"></script>
	</head>
	<body style="display:none;" class="body">
		<div id="portal-container">
			<jsp:include page="${_CONTEXT_.body}" />
		</div>
	</body>
</html>
