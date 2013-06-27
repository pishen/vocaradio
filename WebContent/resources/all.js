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
	
	getInfo();
});

function getInfo(){
	$.get("listen-num", function(data){
		if(data == "null"){
			$("#listen-num").text("N/A");
		}else if(data == "1"){
			$("#listen-num").text("1 listener.");
		}else{
			$("#listen-num").text(data + " listeners.");
		}
	});
	$.get("current", function(data){
		var title = $("#title");
		if(data == "null"){
			title.text("N/A");
		}else if(title.text() != data){
			title.text(data);
		}
	});
	window.setTimeout(getInfo, 20000);
}
