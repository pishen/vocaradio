<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML>
<html>
<head>
<meta charset="utf-8"></meta>
<title>VocaRadio</title>
<link type="text/css" rel="stylesheet" href="resources/style.css" />
<link href='http://fonts.googleapis.com/css?family=Fjalla+One' rel='stylesheet' type='text/css'>
<link rel="icon" type="image/png" href="resources/favicon.png">
</head>
<body>
	<div id="left-bg"></div>
	
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
	
	<div id="right-bg"></div>
	<div id="right-panel">
		<div id="play" class="selected">
			<p>Status: <span id="air">OFF AIR</span> - <span id="listen-num">0 listener.</span></p>
			<p>Now playing: <b id="title"></b></p>
			<audio controls preload="none">
				<source src="http://dg.pishen.info:8000/stream.ogg" type="audio/ogg"></source>
			</audio>
		</div>
		<div id="history">test2</div>
		<div id="order">test3</div>
	</div>
	
	<script src="//ajax.googleapis.com/ajax/libs/jquery/2.0.2/jquery.min.js"></script>
	<script src="resources/all.js"></script>
</body>
</html>