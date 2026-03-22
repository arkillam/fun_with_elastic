<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        pre.doc-content {
            white-space: pre-wrap;
            word-break: break-word;
            background: #fff;
            border: 1px solid #dee2e6;
            border-radius: 0.5rem;
            padding: 1rem;
        }
    </style>
</head>
<body class="bg-light">
<div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h1 class="h4 mb-0">Document</h1>
        <a class="btn btn-outline-secondary" href="/results?q=${fn:escapeXml(query)}">Back to Results</a>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
    </c:if>

    <c:if test="${empty error and not empty document}">
        <div class="card shadow-sm">
            <div class="card-body">
                <h2 class="h5"><c:out value="${document.filename}"/></h2>
                <pre class="doc-content"><c:out value="${document.highlightedContent}" escapeXml="false"/></pre>
            </div>
        </div>

        <c:if test="${document.hasHighlight}">
            <script>
                window.addEventListener('load', function () {
                    var el = document.getElementById('firstHit');
                    if (el) {
                        el.scrollIntoView({behavior: 'smooth', block: 'center'});
                    }
                });
            </script>
        </c:if>
    </c:if>
</div>
</body>
</html>
