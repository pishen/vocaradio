$(document).ready(function() {
	// insert script for ytplayer
	var tag = document.createElement('script');
	tag.src = "https://www.youtube.com/iframe_api";
	var firstScriptTag = document.getElementsByTagName('script')[0];
	firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

	setupUserName();
	setupNotify();
	setupNewMsg();
	updateWS();
	setupAliveMsg();
});

// username
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

// notification
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

// new chat POST
function setupNewMsg() {
	$("#new-msg textarea").keydown(function(e) {
		if (e.keyCode == 13 && $(this).val() != "") {
			var chatLog = {
				user : userName.text(),
				msg : $(this).val()
			};
			$.ajax({
				url : "chat",
				type : "POST",
				data : JSON.stringify(chatLog),
				contentType : "application/json; charset=utf-8"
			});
			$(this).val("");
			return false;
		} else if (e.keyCode == 13) {
			return false;
		}
		document.title = "VocaRadio";
	}).click(function() {
		document.title = "VocaRadio";
	});
}

// websocket
var ws;
var retryTimes = 0;
function updateWS() {
	ws = new WebSocket("ws://" + window.location.host + "/ws");
	ws.onopen = function() {
		// client counter
		ws.send("still alive");
		// chatroom
		$.getJSON("chat-history", function(jsObj) {
			$("#chat-log").children(".old").remove();
			$("#chat-log").prepend(jsObj.history);
			$("#chat-log").scrollTop($("#chat-log").prop("scrollHeight"));
		});
	};
	ws.onmessage = function(e) {
		var json = JSON.parse(e.data);
		if (json.type == "clientCount") {
			$("#client-count").html(json.content);
		} else if (json.type == "chat") {
			$("#chat-log").append(json.content);
			$("#chat-log").scrollTop($("#chat-log").prop("scrollHeight"));
		} else if (json.type == "notify") {
			if ($("#notify").prop("checked") && json.title != userName.text()) {
				var notify = new Notification(json.title, {
					body : json.body,
					icon : "assets/images/logo.png",
					tag : json.tag
				});
				window.setTimeout(function() {
					notify.close();
				}, 5000);
				document.title = "*VocaRadio";
			}
		}
	};
	ws.onclose = function() {
		// chatroom
		$("#chat-log").append("<p>(connection lost，reconnect in 2 sec)</p>");
		$("#chat-log").children().addClass("old");
		$("#chat-log").scrollTop($("#chat-log").prop("scrollHeight"));
		if(retryTimes < 10) window.setTimeout(updateWS, 2000);
		retryTimes += 1;
	};
}

// keep websocket connected by nginx
function setupAliveMsg() {
	window.setInterval(function() {
		ws.send("still alive");
	}, 60000);
}

// YouTube player
var isMobile = /Android|iPhone|iPad|iPod/i.test(navigator.userAgent);
var isIOS = /iPhone|iPad|iPod/i.test(navigator.userAgent);
var player;
function onYouTubeIframeAPIReady() {
	player = new YT.Player('player', {
		height : '360',
		width : '640',
		videoId : '7NptssoOJ78',
		playerVars : {
			'autoplay' : 1,
			'rel' : 0,
			'iv_load_policy' : 3,
			'controls' : isMobile ? 1 : 0,
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
}

var prePlayerState;
var isFirst = true;
function onPlayerStateChange(event) {
	if (event.data == YT.PlayerState.ENDED) {
		syncAndPlay(isFirst);
		isFirst = false;
	} else if (event.data == YT.PlayerState.PLAYING && !isMobile) {
		if (prePlayerState == YT.PlayerState.PAUSED
				|| prePlayerState == YT.PlayerState.ENDED) {
			syncAndPlay(true);
		}
		$("#playback").text("Pause").off().click(function() {
			player.pauseVideo();
		});
	} else if (event.data == YT.PlayerState.PAUSED && !isMobile) {
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
		player.cueVideoById(jsObj.id, seek ? jsObj.start : 0);
		player.setVolume($("#volume").prop("value"));
		if (!isIOS)
			player.playVideo();
	});
}