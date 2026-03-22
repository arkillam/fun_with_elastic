<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Results</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<div class="container py-4">
    <div class="row mb-3">
        <div class="col-12">
            <form action="/results" method="get" class="d-flex gap-2">
                <input type="text" class="form-control" name="q" value="${fn:escapeXml(query)}" placeholder="Search again..." required>
                <button class="btn btn-primary" type="submit">Search</button>
                <a class="btn btn-outline-secondary" href="/search">New Search</a>
            </form>
        </div>
    </div>

    <div class="row mb-3">
        <div class="col-12">
            <h1 class="h4 mb-0">Results for &ldquo;<c:out value="${query}"/>&rdquo;</h1>
            <p class="text-muted mb-0">${totalHits} matches</p>
        </div>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-warning" role="alert"><c:out value="${error}"/></div>
    </c:if>

    <c:if test="${empty error}">
        <c:if test="${empty results}">
            <div class="alert alert-info" role="alert">No results found.</div>
        </c:if>

        <c:forEach var="item" items="${results}">
            <div class="card mb-3 shadow-sm">
                <div class="card-body">
                    <h2 class="h5 mb-2">
                        <a href="/document?filename=${fn:escapeXml(item.filename)}&q=${fn:escapeXml(query)}"
                           class="text-decoration-none"><c:out value="${item.filename}"/></a>
                    </h2>
                    <p class="mb-0 text-muted"><c:out value="${item.snippet}"/></p>
                </div>
            </div>
        </c:forEach>

        <c:if test="${totalPages > 1}">
            <nav aria-label="Search results pages">
                <ul class="pagination flex-wrap">
                    <li class="page-item ${currentPage <= 1 ? 'disabled' : ''}">
                        <a class="page-link" href="/results?q=${fn:escapeXml(query)}&page=${currentPage - 1}">Previous</a>
                    </li>

                    <c:forEach var="pageNum" begin="${startPage}" end="${endPage}">
                        <li class="page-item ${pageNum == currentPage ? 'active' : ''}">
                            <a class="page-link" href="/results?q=${fn:escapeXml(query)}&page=${pageNum}">${pageNum}</a>
                        </li>
                    </c:forEach>

                    <li class="page-item ${currentPage >= totalPages ? 'disabled' : ''}">
                        <a class="page-link" href="/results?q=${fn:escapeXml(query)}&page=${currentPage + 1}">Next</a>
                    </li>
                </ul>
            </nav>
        </c:if>
    </c:if>
</div>
</body>
</html>
