$(document).ready(function() {
	setupFB()
	setupRequests()
})

function setupRequests() {
	$("#status-error").click(function() {
		$.post("status-error", JSON.stringify({
			token : accessToken
		}), showResponse)
	})
	$("#song-by-id").click(function() {
		$.post("song-by-id", JSON.stringify({
			token : accessToken,
			videoId : $("#entries>input").val()
		}), showResponse)
	})
	$("#song-by-title").click(function() {
		$.post("song-by-title", JSON.stringify({
			token : accessToken,
			originTitle : $("#entries>input").val()
		}), showResponse)
	})
}

function showResponse(data) {
	$("#content").html(data)
	$(".update").click(function() {
		var ul = $(this).closest("ul")
		$.post("update-video-id", JSON.stringify({
			token : accessToken,
			nodeId : ul.attr("id"),
			newVideoId : ul.find("input").val()
		}), function(data) {
			ul.find("li.video-id").html(data)
		})
	})
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
		FB.Event.subscribe('auth.authResponseChange', function(resp) {
			if (resp.status === 'connected') {
				$("#entries").append("connected")
				accessToken = resp.authResponse.accessToken
			}
		})
	})
}
