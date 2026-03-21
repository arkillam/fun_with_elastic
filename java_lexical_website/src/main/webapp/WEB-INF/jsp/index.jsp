<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hello World</title>
    <style>
        body {
            margin: 0;
            min-height: 100vh;
            display: grid;
            place-items: center;
            font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(120deg, #f0f4ff, #e4fff3);
            color: #1f2d3d;
        }
        .card {
            background: #ffffff;
            border-radius: 12px;
            padding: 2rem 2.5rem;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
            text-align: center;
        }
        h1 {
            margin: 0;
            font-size: 2rem;
        }
    </style>
</head>
<body>
    <main class="card">
        <h1>${message}</h1>
    </main>
</body>
</html>
