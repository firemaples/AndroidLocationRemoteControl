var div_mapDiv = $("#div_mapDiv"), tv_position = $("#tv_position");
var map, marker;
var mapLoaded = false;
$(function () {

});

function loadGoogleMapApi(apiKey, callback) {
    div_mapBody.show();
    if (!mapLoaded) {
        $.getScript("https://maps.googleapis.com/maps/api/js?callback=mapInit&key=" + apiKey)
            .done(function (evt) {
                mapLoaded = true;
                callback();
            })
            .fail(function (evt) {

            });
    } else {
        callback();
    }
}

function mapInit() {
    console.log("mapInit");
    var mapOptions = {
        center: new google.maps.LatLng(24.194509, 120.699003),
        zoom: 17
    };
    map = new google.maps.Map(document.getElementById('div_mapCanvas'),
        mapOptions);
    google.maps.event.addListener(map, 'click', function (event) {
        var lat = event.latLng.lat();
        var lng = event.latLng.lng();
        tv_position.html(lat + ", " + lng);

        if (marker == null) {
            marker = new google.maps.Marker({
                position: event.latLng,
                map: map,
                title: 'Selected Position'
            });
        } else {
            marker.setPosition(event.latLng);
        }

        sendPosition(event.latLng);
    });
}

function sendPosition(latLng) {
    sendMsg("geo:" + latLng.lat() + "," + latLng.lng());
}
