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

var webSocket;
function updateWebSocket() {
	webSocket = new WebSocket("ws://" + window.location.host + "/ws");
	webSocket.onopen = function(){
		webSocket.send("join");
	};
	webSocket.onmessage = function(e){
		$("#client-count span").text(e.data)
	};
	webSocket.onerror = function(error){
		console.log("ws error: " + error);
	};
	webSocket.onclose = function(e){
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