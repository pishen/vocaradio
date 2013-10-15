$(document).ready(function() {
	var tag = document.createElement('script');
	tag.src = "https://www.youtube.com/iframe_api";
	var firstScriptTag = document.getElementsByTagName('script')[0];
	firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

	updateWebSocket();

	$("#sync").on("click", function() {
		$.getJSON("sync", function(jsObj) {
			player.loadVideoById(jsObj.id, jsObj.start);
		});
	});
});

var name = Math.random().toString(36).substring(7);
var webSocket;
function updateWebSocket() {
	webSocket = new WebSocket("ws://" + window.location.host + "/ws");
	webSocket.onopen = function() {
		var msg = {type: "join"};
		webSocket.send(JSON.stringify(msg));
	};
	webSocket.onmessage = function(e) {
		var jsObj = JSON.parse(e.data);
		switch (jsObj.type) {
		case "client-count":
			var follow = jsObj.value <= 1 ? " listener" : " listeners";
			$("#client-count").html("<span>" + jsObj.value + "</span>" + follow);
			break;
		case "chat":
			console.log(jsObj.value);
			break;
		}
	};
	webSocket.onerror = function(error) {
		console.log("ws error: " + error);
	};
	webSocket.onclose = function(e) {
		window.setTimeout(updateWebSocket, 2000);
	};
}

var player;
function onYouTubeIframeAPIReady() {
	$.getJSON("sync", function(jsObj) {
		player = new YT.Player('player', {
			height : '360',
			width : '640',
			videoId : jsObj.id,
			playerVars : {
				'autoplay' : 1,
				'start' : jsObj.start,
				'rel' : 0,
				'iv_load_policy' : 3
			},
			events : {
				'onStateChange' : onPlayerStateChange
			}
		});
	});
}

function onPlayerStateChange(event) {
	if (event.data == YT.PlayerState.ENDED) {
		$.getJSON("sync", function(jsObj) {
			player.loadVideoById(jsObj.id, 0);
			console.log("origin title: " + jsObj.originTitle)
		});
	}
}