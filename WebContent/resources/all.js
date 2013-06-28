var errorCount = 0;
var fadingOn = false;

$(document).ready(function(){
	$("nav li").on("click", function(){
		$("nav li.selected").removeClass("selected");
		$("#right-panel > div.selected").removeClass("selected");
		$(this).addClass("selected");
		$("#right-panel > div").eq($(this).index()).addClass("selected");
	});
	
	$("audio").on("error", function(e){
		errorCount++;
		if(errorCount < 5){
			this.load();
			this.play();
		}
		window.setTimeout(function(){
			errorCount--;
		}, 10000);
	});
	
	$("audio").on("pause", function(e){
		var source = $("audio source");
		var temp = source.src;
		source.src = "";
		this.load();
		source.src = temp;
	});
	
	getStatus();
	//fadeStatus();
});

function getStatus(){
	$.getJSON("status", function(data){		
		if(data.onAir == "false"){
			$("#air").toggleClass("on", false).text("OFF AIR");
			$("#listen-num").text("0 listener.");
			$("#title").text("--");
		}else{
			$("#air").toggleClass("on", true).text("ON AIR");
			if(data.num == "0" || data.num == "1"){
				$("#listen-num").text(data.num + " listener.");
			}else{
				$("#listen-num").text(data.num + " listeners.");
			}
			if($("#title").text() != data.title){
				$("#title").text(data.title);
			}
		}
	});
	window.setTimeout(getStatus, 12000);
}

/*function fadeStatus(){
	if($("#air").hasClass("on")){
		$("#air").fadeTo('slow', 0.6, function(){
			$(this).fadeTo('slow', 1.0);
			fadeStatus();
		});
	}else{
		window.setTimeout(fadeStatus, 1000);
	}
}*/
