$(document).ready(function() {
	setupFB()
	setupRequests()
})

function setupRequests() {
	$("#status-error").click(function() {
		$.post("vocaRequest", JSON.stringify({
			action : "status-error"
		}), function(data) {
			$("#content").html(data)
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
				window.alert("connected")
				accessToken = resp.authResponse.accessToken
			}
		})
	})
}
