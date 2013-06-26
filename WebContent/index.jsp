<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML>
<html>
<head>
<meta charset="utf-8"></meta>
<title>VOCARADIO</title>
<link type="text/css" rel="stylesheet" href="resources/style.css" />
<link href='http://fonts.googleapis.com/css?family=Fjalla+One' rel='stylesheet' type='text/css'>
</head>
<body>
	<div id="bg"></div>
	
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
			<audio controls preload="none">
				<source src="http://dg.pishen.info:8000/stream.ogg" type="audio/ogg"></source>
			</audio>
		</div>
		<div id="history">test2</div>
		<div id="order">test3</div>
	</div>
	<div id="fill-bg"></div>
	
	<script src="http://code.jquery.com/jquery-2.0.0.min.js"></script>
	<script src="resources/all.js"></script>
</body>
</html>