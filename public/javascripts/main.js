$(document).ready(function() {
	var tag = document.createElement('script');
	tag.src = "https://www.youtube.com/iframe_api";
	var firstScriptTag = document.getElementsByTagName('script')[0];
	firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

	$("#sync").on("click", function() {
		$.getJSON("sync", function(jsObj) {
			player.loadVideoById(jsObj.id, jsObj.start);
			console.log("origin title: " + jsObj.originTitle)
		});
	});
});

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
		console.log("origin title: " + jsObj.originTitle)
	});
}

function onPlayerStateChange(event) {
	if (event.data == YT.PlayerState.ENDED) {
		$.getJSON("sync", function(jsObj) {
			player.loadVideoById(jsObj.id);
			console.log("origin title: " + jsObj.originTitle)
		});
	}
}