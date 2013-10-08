$(document).ready(function() {
	var tag = document.createElement('script');
	tag.src = "https://www.youtube.com/iframe_api";
	var firstScriptTag = document.getElementsByTagName('script')[0];
	firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
	
	$("#sync").on("click", function(){
		player.loadVideoById('WiUjG9fF3zw', 20);
	});
});

var player;
function onYouTubeIframeAPIReady() {
	player = new YT.Player('player', {
		height : '360',
		width : '640',
		videoId : '243vPl8HdVk',
		playerVars : {
			'autoplay' : 1,
			'start' : 20
		},
		events : {
			'onReady' : onPlayerReady,
			'onStateChange' : onPlayerStateChange
		}
	});
}

function onPlayerReady(event) {
}

function onPlayerStateChange(event) {
	if(event.data == YT.PlayerState.ENDED){
		player.loadVideoById('z0KPuuAz0Yc');
	}
}