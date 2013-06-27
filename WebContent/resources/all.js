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
		if(errorCount < 5){
			this.load();
			this.play();
		}
		window.setTimeout(function(){
			errorCount--;
		}, 10000);
	});
	
	getTitle();
});

function getTitle(){
	$.get("current", function(data){
		var title = $("#title");
		if(data == null){
			title.text("");
		}else if(title.text() != data){
			title.text(data);
		}
	});
	window.setTimeout(getTitle, 20000);
}
