var originTitle
$(document).ready(function() {
	if (window.WebSocket){
		// insert script for ytplayer
		var tag = document.createElement('script')
		tag.src = "https://www.youtube.com/iframe_api"
		var firstScriptTag = document.getElementsByTagName('script')[0]
		firstScriptTag.parentNode.insertBefore(tag, firstScriptTag)

		originTitle = document.title
		
		setupNotify()
		setupUserName()
		setupNewMsg()
		updateWS()
		setupFB()
	}else{
		window.alert("No support for websocket, please update your browser!")
	}
})

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
		}
	})
}

// username
var userName
function setupUserName() {
	userName = $("#username")
	if (localStorage.userName) {
		userName.val(localStorage.userName)
	}
	userName.blur(function() {
		localStorage.userName = userName.val()
	})

}

function openSetting() {
	location.hash = "#setting"
	if (!userName.val()) {
		userName.select()
	}
}

// new chat POST
function setupNewMsg() {
	$("#new-msg textarea").keydown(function(e) {
		if (e.keyCode == 13 && $(this).val() != "") {
			var chatLog = {
				name : userName.val(),
				token : accessToken ? accessToken : "guest",
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
	$("#new-msg").focusin(function() {
		if (!userName.val()) {
			openSetting()
		}
	})
}

// playlist
var list
function updatePlaylist(json) {
	if(!isMobile){
		if (json) {
			if (json.append) {
				var itemToAppend = $(json.append)
				itemToAppend.appendTo("#playlist")
			} else if (json.convert) {
				$.each(json.convert, function(i, json2) {
					var song = $("#" + json2.id)
					if (json2.remove) {
						song.remove()
					} else {
						song.css({
							"transform" : json2.to,
							"-webkit-transform" : json2.to
						})
						if (json2.orderedBy) {
							song.find(".order").off().remove()
							song.find(".overlap").append(json2.orderedBy)
						}
					}
				})
			}
			setupOrders()
		} else {
			$.getJSON("listContent", function(json) {
				$("#playlist").empty().append(json.content)
				setupOrders()
			})
		}
	}
}

function setupOrders() {
	$(".order").off().click(function() {
		if (!userName.val() || !accessToken) {
			openSetting()
		} else {
			var order = {
				name : userName.val(),
				token : accessToken,
				videoId : $(this).closest(".song").attr("id")
			}
			$.ajax({
				url : "order",
				type : "POST",
				data : JSON.stringify(order),
				contentType : "application/json; charset=utf-8"
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
		ws.send("Hello, Voca")
		// chatroom
		$.getJSON("chat-history", function(jsObj) {
			$("#chat-log").empty().append(jsObj.history)
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
		} else if (json.type == "updatePlaylist") {
			updatePlaylist(json)
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
		$("#chat-log").scrollTop($("#chat-log").prop("scrollHeight"))
		if (retryTimes < 5) {
			setTimeout(updateWS, 2000)
			retryTimes += 1
		}
	}
}

// Mobile test
var isMobile = /Android|iPhone|iPad|iPod/i.test(navigator.userAgent)
var isIOS = /iPhone|iPad|iPod/i.test(navigator.userAgent)

// YouTube player
var player
function onYouTubeIframeAPIReady() {
	player = new YT.Player('player', {
		height : '360',
		width : '640',
		videoId: '7NptssoOJ78',
		playerVars : {
			'wmode' : 'opaque',
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
	if (localStorage.volume)
		$("#volume").prop("value", localStorage.volume)
	player.setVolume($("#volume").prop("value"))
	$("#volume").change(function() {
		player.setVolume($(this).prop("value"))
		localStorage.volume = $(this).prop("value")
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
	if (window.WebSocket){
		$.getJSON("sync", function(json) {
			console.log("sync id: " + json.id)
			player.loadVideoById(json.id, seek ? json.position : 0)
			player.setVolume($("#volume").prop("value"))
		})
	}
}

// Facebook
var accessToken
function setupFB() {
	$.getScript('//connect.facebook.net/en_US/all.js', function() {
		FB.init({
			appId : '565192206888536',
			status : true,
			cookie : true,
			xfbml : false
		})
		$("#login").click(function() {
			FB.login()
		})
		$("#logout").click(function() {
			FB.logout()
		}).hide()
		FB.Event.subscribe('auth.authResponseChange', function(resp) {
			if (resp.status === 'connected') {
				$("#login").hide()
				$("#logout").show()
				accessToken = resp.authResponse.accessToken
				FB.api('/me', function(resp) {
					$("#fb-status").text(" (Logged in as " + resp.name + ") ")
				})
			} else {
				$("#login").show()
				$("#logout").hide()
				$("#fb-status").text("")
			}
		})
	})
}
