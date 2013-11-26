$(document).ready(function() {
	// insert script for ytplayer
	var tag = document.createElement('script');
	tag.src = "https://www.youtube.com/iframe_api";
	var firstScriptTag = document.getElementsByTagName('script')[0];
	firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

	updateWsCounter();
	setupUserName();
	setupNotify();
	updateWsChat();
});

// websocket chatroom
var userName;
function setupUserName() {
	userName = $("#username");
	if (typeof (Storage) !== "undefined" && localStorage.userName) {
		userName.text(localStorage.userName);
	} else {
		$("#new-msg").hide();
		insertNewNameInput("");
	}
	userName.click(function() {
		var oldNameStr = $(this).text();
		$(this).hide();
		insertNewNameInput(oldNameStr);
	});
}

function insertNewNameInput(oldNameStr) {
	$("<input>").val(oldNameStr).keyup(function(e) {
		if (e.keyCode == 13) {
			var newNameStr = $(this).val();
			if (newNameStr != "") {
				userName.text(newNameStr).show();
				localStorage.userName = newNameStr;
				$(this).remove();
				if (oldNameStr == "")
					$("#new-msg").slideDown();
			}
		} else if (e.keyCode == 27) {
			if (oldNameStr != "") {
				userName.show();
				$(this).remove();
			}
		}
	}).insertAfter(userName).select();
}

function setupNotify() {
	$("#notify").change(function() {
		if ($(this).prop("checked")) {
			Notification.requestPermission(function() {
				var notify = new Notification("VocaRadio", {
					body : "訊息通知已啟動",
					icon : "assets/images/logo.png"
				});
				window.setTimeout(function() {
					notify.close();
				}, 5000);
			});
			$(".tip").remove();
		}
	});
}

function chatLogToHtml(logJsObj) {
	return "<strong>" + logJsObj.user + "</strong>" + "<p>" + logJsObj.msg
			+ "</p>";
}

function getHistory() {
	$.getJSON("chat-history", function(jsObj) {
		$("#chat-log").children(".old").remove();
		var logs = jsObj.logs;
		for (var i = 0; i < logs.length; i++) {
			var log = logs[i];
			$("#chat-log").prepend(chatLogToHtml(log));
		}
		$("#chat-log").scrollTop($("#chat-log").prop("scrollHeight"));
	});
}

var wsChat;
var wsChatReconnect = false;
function updateWsChat() {
	wsChat = new WebSocket("ws://" + window.location.host + "/ws/chat");
	wsChat.onopen = function() {
		getHistory();
		$("#new-msg textarea").off().keydown(function(e) {
			if (e.keyCode == 13 && $(this).val() != "") {
				var log = {
					user : userName.text(),
					msg : $(this).val()
				};
				wsChat.send(JSON.stringify(log));
				$(this).val("");
				return false;
			}
			document.title = "VocaRadio";
		}).click(function() {
			document.title = "VocaRadio";
		});
	};
	wsChat.onmessage = function(e) {
		var log = JSON.parse(e.data);
		$("#chat-log").append(chatLogToHtml(log));
		$("#chat-log").scrollTop($("#chat-log").prop("scrollHeight"));
		if ($("#notify").prop("checked")) {
			var notify = new Notification(log.user, {
				body : log.msg,
				icon : "assets/images/logo.png",
				tag : "chat"
			});
			window.setTimeout(function() {
				notify.close();
			}, 5000);
		}
		document.title = "*VocaRadio";
	};
	wsChat.onclose = function() {
		$("#chat-log").append("<p>(connection lost, reconnect in 2 secs)</p>");
		$("#chat-log").children().addClass("old");
		$("#chat-log").scrollTop($("#chat-log").prop("scrollHeight"));
		wsChatReconnect = true;
		window.setTimeout(updateWsChat, 2000);
	};
}

// client counter
var wsCounter;
function updateWsCounter() {
	wsCounter = new WebSocket("ws://" + window.location.host + "/ws/counter");
	wsCounter.onopen = function() {
		wsCounter.send("hi");
		window.setInterval(function() {
			wsCounter.send("still alive");
		}, 300000);
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

// YouTube player
var player;
function onYouTubeIframeAPIReady() {
	player = new YT.Player('player', {
		height : '360',
		width : '640',
		playerVars : {
			'rel' : 0,
			'iv_load_policy' : 3,
			'controls' : 1,
			'autohide' : 1,
			'disablekb' : 1
		},
		events : {
			'onReady' : onPlayerReady,
			'onStateChange' : onPlayerStateChange,
			'onError' : onPlayerError
		}
	});
}

function onPlayerReady(event) {
	player.setVolume($("#volume").prop("value"));
	$("#volume").change(function() {
		player.setVolume($(this).prop("value"));
	});
	syncAndPlay(true);
}

var prePlayerState;
function onPlayerStateChange(event) {
	if (event.data == YT.PlayerState.ENDED) {
		syncAndPlay(false);
	} else if (event.data == YT.PlayerState.PLAYING) {
		if (prePlayerState == YT.PlayerState.PAUSED
				|| prePlayerState == YT.PlayerState.ENDED) {
			syncAndPlay(true);
		}
		$("#playback").text("Pause").off().click(function() {
			player.pauseVideo();
		});
	} else if (event.data == YT.PlayerState.PAUSED) {
		$("#playback").text("Play").off().click(function() {
			syncAndPlay(true);
		});
	}
	prePlayerState = event.data;
}

function onPlayerError(event) {
	console.log("player error: " + event.data);
	$("#playback").text("Play").off().click(function() {
		syncAndPlay(true);
	});
}

function syncAndPlay(seek) {
	console.log("seek: " + seek);
	$.getJSON("sync", function(jsObj) {
		player.loadVideoById(jsObj.id, seek ? jsObj.start : 0);
		player.setVolume($("#volume").prop("value"));
	});
}