function uploadFile() {
    var formData = new FormData();
    formData.append("sourceFile", $("#csv_file")[0].files[0]);
    formData.append("col", $("#col_size").val());

    $.ajax("/file/upload", {
        type: "POST",
        data: formData,
        timeout: 30000,
        enctype: "multipart/form-data",
        processData: false,
        contentType: false,
        cache: false,
    }).done(function(data, textStatus, jqXHR) {
        console.log(data);
        var obj = JSON.parse(data);
        window.open("/file/download?filepath=" + obj.url)
    }).fail(function(jqXHR, textStatus, errorThrown) {
        console.log(errorThrown);
    });
}

$(document).ready(function() {
    $(".btn-submit-file").on("click", uploadFile);
});