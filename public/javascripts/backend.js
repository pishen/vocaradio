$("#shift-btn").click(function() {
    $.post("/shift")
})

$("#kick-btn").click(function() {
    $.post("/kick/" + $("#kick-vid").val())
})

$("#search-by-key-btn").click(function() {
    $.get("/song/key/" + $(this).prev().val(), function(data) {
        $("#table").html(data)
    })
})

$("#search-by-id-btn").click(function() {
    $.get("/song/id/" + $(this).prev().val(), function(data) {
        $("#table").html(data)
    })
})

$("#get-not-founds-btn").click(function() {
    $.get("/song/notFounds", function(data) {
        $("#table").html(data)
    })
})

$("#set-id-btn").click(function() {
    var key = $(this).prev().prev().val()
    var newId = $(this).prev().val()
    $.post("/setSongId", {
        key: key,
        newId: newId
    }, function() {
        $("#table .key").filter(function() {
            return $(this).text() == key
        }).next().text(newId)
    })

})

$("#keys-to-merge-form").submit(function(e){
    $.ajax({
        url: "/mergeKeys",
        type: "POST",
        data: new FormData(this),
        processData: false,
        contentType: false
    })
    e.preventDefault()
})
