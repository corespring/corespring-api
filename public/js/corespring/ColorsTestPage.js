$.ajax({
  url: "/api/v2/organizations/display-config",
  dataType: "text",
  success: function(data) {
    console.log(data);
    $(".colorsJson").val(data);
  }
});

$('.submitButton').click(function() {
  var colorsJson = $(".colorsJson").val();
  $.ajax({
    type: 'PUT',
    url: "/api/v2/organizations/display-config",
    contentType: 'application/json',
    data: colorsJson
  });
});