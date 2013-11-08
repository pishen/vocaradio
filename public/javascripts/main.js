$(document).ready(function() {
	var tag = document.createElement('script');
	tag.src = "https://www.youtube.com/iframe_api";
	var firstScriptTag = document.getElementsByTagName('script')[0];
	firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

	updateWsCounter();

	$("#sync").on("click", function() {
		$.getJSON("sync", function(jsObj) {
			player.loadVideoById(jsObj.id, jsObj.start);
		});
	});

	setupUserName();
	getHistory();
	updateWsChat();
});

var userName;
function setupUserName() {
	userName = $("#user-name strong");
	userName.text("(" + Math.random().toString(36).substring(7) + ")");
	userName.on("click", function() {
		var userNameStr = $(this).text();
		$(this).hide();
		$("#user-name input").val(userNameStr).show().select();
	});

	$("#user-name input").keyup(function(e) {
		if (e.keyCode == 13) {
			var newNameStr = $(this).val();
			if (newNameStr != "" && newNameStr != null) {
				userName.text(newNameStr).show();
				$(this).hide();
			}
		} else if (e.keyCode == 27) {
			userName.show();
			$(this).hide();
		}
	});
}

function chatLogToHtml(logJsObj) {
	return "<strong>" + logJsObj.user + "</strong>" + "<p>" + logJsObj.msg + "</p>"
}

function getHistory() {
	$.getJSON("chat-history", function(jsObj) {
		var logs = jsObj.logs;
		for(var i = 0; i < logs.length; i++){
			var log = logs[i];
			$("#chat-log").prepend(chatLogToHtml(log));
		}
		$("#chat-log").scrollTop($("#chat-log").prop("scrollHeight"));
	});
}

var wsChat;
function updateWsChat() { 
	wsChat = new WebSocket("ws://" + window.location.host + "/ws/chat");
	wsChat.onopen = function() {
		$("#new-msg textarea").keydown(function(e) {
			if (e.keyCode == 13 && $(this).val() != "") {
				var log = {
					user: userName.text(), 
					msg: $(this).val()
				};
				wsChat.send(JSON.stringify(log));
				$(this).val("");
				return false;
			}
		});
	};
	wsChat.onmessage = function(e) {
		var log = JSON.parse(e.data);
		$("#chat-log").append(chatLogToHtml(log));
		$("#chat-log").scrollTop($("#chat-log").prop("scrollHeight"));
	};
	wsChat.onclose = function() {
		window.setTimeout(updateWsChat, 2000);
	};
}

var wsCounter;
function updateWsCounter() {
	wsCounter = new WebSocket("ws://" + window.location.host + "/ws/counter");
	wsCounter.onopen = function() {
		wsCounter.send("ready");
	};
	wsCounter.onmessage = function(e) {
		var jsObj = JSON.parse(e.data);
		var follow = jsObj.clientCount <= 1 ? " listener" : " listeners";
		$("#client-count").html(
				"<span>" + jsObj.clientCount + "</span>" + follow);
	};
	wsCounter.onclose = function() {
		window.setTimeout(updateWsCounter, 2000);
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
				'onReady' : onPlayerReady,
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

function onPlayerReady(event) {
	player.setVolume(50);
}