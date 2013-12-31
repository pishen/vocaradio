var originTitle
$(document).ready(function() {
	// insert script for ytplayer
	var tag = document.createElement('script')
	tag.src = "https://www.youtube.com/iframe_api"
	var firstScriptTag = document.getElementsByTagName('script')[0]
	firstScriptTag.parentNode.insertBefore(tag, firstScriptTag)

	originTitle = document.title

	setupUserName()
	setupNotify()
	setupNewMsg()
	updateWS()
	setupAliveMsg()
	setupFB()

})

// username
var userName
function setupUserName() {
	userName = $("#username")
	if (typeof (Storage) !== "undefined" && localStorage.userName) {
		userName.val(localStorage.userName)
	}
	userName.blur(function() {
		localStorage.userName = userName.val()
	})
	$("#new-msg").focusin(function() {
		if (!userName.val()) {
			location.hash = "#setting"
			userName.select()
		}
	})
}

// notification
function setupNotify() {
	$("#notify").change(function() {
		if ($(this).prop("checked")) {
			Notification.requestPermission(function() {
				var notify = new Notification("VocaRadio", {
					body : "訊息通知已啟動",
					icon : "assets/images/logo.png"
				})
				window.setTimeout(function() {
					notify.close()
				}, 5000)
			})
			$(".tip").remove()
		}
	})
}

// new chat POST
function setupNewMsg() {
	$("#new-msg textarea").keydown(function(e) {
		if (e.keyCode == 13 && $(this).val() != "") {
			var chatLog = {
				user : userName.val(),
				msg : $(this).val()
			}
			$.ajax({
				url : "chat",
				type : "POST",
				data : JSON.stringify(chatLog),
				contentType : "application/json; charset=utf-8"
			})
			$(this).val("")
			return false
		} else if (e.keyCode == 13) {
			return false
		}
		document.title = originTitle
	}).click(function() {
		document.title = originTitle
	})
}

// playlist
function updatePlaylist() {
	$.getJSON("listContent", function(jsObj) {
		var playlist = $("#playlist")
		var imgs = playlist.children()
		if (imgs.length == 0) {
			$.each(jsObj.imgs, function(i, imgStr) {
				$(imgStr).appendTo(playlist)
			})
		} else {
			imgs.each(function(i) {
				var newImg = $(jsObj.imgs[i]).hide()
				$(this).delay(i * 100).fadeOut(function() {
					$(this).replaceWith(newImg)
					newImg.fadeIn()
				})
			})
		}
	})
}

// websocket
var ws
var retryTimes = 0
function updateWS() {
	ws = new WebSocket("ws://" + window.location.host + "/ws")
	ws.onopen = function() {
		retryTimes = 0
		// client counter
		ws.send("still alive")
		// chatroom
		$.getJSON("chat-history", function(jsObj) {
			$("#chat-log").children(".old").remove()
			$("#chat-log").prepend(jsObj.history)
			$("#chat-log").scrollTop($("#chat-log").prop("scrollHeight"))
		})
		updatePlaylist()
	}
	ws.onmessage = function(e) {
		var json = JSON.parse(e.data)
		if (json.type == "clientCount") {
			$("#client-count").html(json.content)
		} else if (json.type == "chat") {
			$("#chat-log").append(json.content)
			$("#chat-log").scrollTop($("#chat-log").prop("scrollHeight"))
		} else if (json.type == "updateList") {
			updatePlaylist()
		} else if (json.type == "notify") {
			if ($("#notify").prop("checked") && json.title != userName.val()) {
				var notify = new Notification(json.title, {
					body : json.body,
					icon : "assets/images/logo.png",
					tag : json.tag
				})
				window.setTimeout(function() {
					notify.close()
				}, 5000)
				document.title = "*" + originTitle
			}
		}
	}
	ws.onclose = function() {
		// chatroom
		$("#chat-log").append("<p>(connection lost，reconnect in 2 sec)</p>")
		$("#chat-log").children().addClass("old")
		$("#chat-log").scrollTop($("#chat-log").prop("scrollHeight"))
		if (retryTimes < 10) {
			window.setTimeout(updateWS, 2000)
			retryTimes += 1
		}
	}
}

// keep websocket connected by nginx
function setupAliveMsg() {
	window.setInterval(function() {
		ws.send("still alive")
	}, 60000)
}

// YouTube player
var isMobile = /Android|iPhone|iPad|iPod/i.test(navigator.userAgent)
var isIOS = /iPhone|iPad|iPod/i.test(navigator.userAgent)
var player
function onYouTubeIframeAPIReady() {
	player = new YT.Player('player', {
		height : '360',
		width : '640',
		playerVars : {
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
	})
}

function onPlayerReady(event) {
	console.log("ready")
	player.setVolume($("#volume").prop("value"))
	$("#volume").change(function() {
		player.setVolume($(this).prop("value"))
	})
	if (isFirst)
		player.loadVideoById('7NptssoOJ78')
	else
		syncAndPlay(true)
}

var prePlayerState
var isFirst = true
function onPlayerStateChange(event) {
	if (event.data == YT.PlayerState.ENDED) {
		console.log("ended")
		syncAndPlay(isFirst)
		isFirst = false
	} else if (event.data == YT.PlayerState.PLAYING && !isMobile) {
		console.log("playing")
		if (prePlayerState == YT.PlayerState.PAUSED
				|| prePlayerState == YT.PlayerState.ENDED) {
			syncAndPlay(true)
		}
		$("#playback").text("Pause").off().click(function() {
			player.pauseVideo()
		})
	} else if (event.data == YT.PlayerState.PAUSED && !isMobile) {
		$("#playback").text("Play").off().click(function() {
			syncAndPlay(true)
		})
	}
	prePlayerState = event.data
}

function onPlayerError(event) {
	console.log("player error: " + event.data)
	$("#playback").text("Play").off().click(function() {
		syncAndPlay(true)
	})
}

function syncAndPlay(seek) {
	// console.log("seek: " + seek)
	$.getJSON("sync", function(jsObj) {
		console.log("sync id: " + jsObj.id)
		player.loadVideoById(jsObj.id, seek ? jsObj.start : 0)
		player.setVolume($("#volume").prop("value"))
	})
}

// Facebook
function setupFB() {
	$.getScript('//connect.facebook.net/en_US/all.js', function() {
		FB.init({
			appId : '565192206888536',
			status : true,
			cookie : true,
			xfbml  : false
		})
		$("#login").click(function(){
			FB.login()
		})
		$("#logout").click(function(){
			FB.logout()
		}).hide()
		FB.Event.subscribe('auth.authResponseChange', function(resp) {
			if (resp.status === 'connected') {
				$("#login").hide()
				$("#logout").show()
				FB.api('/me', function(resp) {
					$("#fb-status").text(" (Logged in as " + resp.name + ") ")
				})
			}else{
				$("#login").show()
				$("#logout").hide()
				$("#fb-status").text("")
			}
		})
	})
}
