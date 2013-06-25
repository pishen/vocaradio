<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML>
<html>
<head>
<meta charset="utf-8"></meta>
<title>VOCARADIO</title>
<link type="text/css" rel="stylesheet" href="resources/style.css" />
<link href='http://fonts.googleapis.com/css?family=Fjalla+One' rel='stylesheet' type='text/css'>
<script src="resources/menu.js"></script>
</head>
<body onload="bodyLoad()">
	<div id="bg">
		
	</div>
	
	<div id="left-panel">
		<h1>VocaRadio</h1>
		<nav>
			<ul>
				<li class="selected">Play</li>
				<li>History</li>
				<li>Order</li>
			</ul>
		</nav>
	</div>
	
	<div id="right-panel">
		<div id="play" class="selected">
			<p>Status: <span id="status">ON AIR</span> - <b>1/20</b> listeners.</p>
			<p>Now playing: <b>タイトル</b></p>
			<audio src="http://dg.pishen.info:8000/stream.ogg" controls></audio>
		</div>
		<div id="history">test2</div>
		<div id="order">test3</div>
	</div>
	<div id="fill-bg"></div>
</body>
</html>