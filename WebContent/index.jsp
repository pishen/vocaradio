<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML>
<html>
<head>
<meta charset="utf-8"></meta>
<title>VocaRadio</title>
<link rel="stylesheet" href="resources/style.css" />
<link rel="stylesheet" href="resources/persona-buttons.css" />
<link href='http://fonts.googleapis.com/css?family=Fjalla+One' rel='stylesheet' type='text/css'>
<link rel="icon" type="image/png" href="resources/favicon.png">
</head>
<body>
	<div id="left-bg"></div>
	<div id="left-panel">
		<h1>VocaRadio</h1>
		<nav>
			<ul>
				<li class="selected">Listen</li>
				<li>Order</li>
				<li>Chat</li>
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
		<div id="order"></div>
		<div id="chat">
			<div id="chat-log">
				<!-- <p><span class="username"></span><span class="chat-content"></span></p> -->
			</div>
			<textarea id="new-chat" placeholder="Your message..."></textarea>
		</div>
	</div>
	
	<script src="https://login.persona.org/include.js"></script>
	<script src="http://code.jquery.com/jquery-2.0.3.min.js"></script>
	<script src="resources/all.js"></script>
</body>
</html>