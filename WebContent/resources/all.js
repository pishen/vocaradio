var errorCount = 0;

$(document).ready(function(){
	$("nav li").on("click", function(){
		$("nav li.selected").removeClass("selected");
		$("#right-panel > div.selected").removeClass("selected");
		$(this).addClass("selected");
		$("#right-panel > div").eq($(this).index()).addClass("selected");
	});
	
	$("audio").on("error", function(e){
		errorCount++;
		//console.log("errorCount: " + errorCount);
		if(errorCount < 3){
			this.load();
			this.play();
		}
		window.setTimeout(function(){
			errorCount--;
			//console.log("errorCount: " + errorCount);
		}, 10000);
	});
});
