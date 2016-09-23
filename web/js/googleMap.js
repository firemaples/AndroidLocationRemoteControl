var div_mapDiv = $("#div_mapDiv")
    , tv_position = $("#tv_position")
    , tv_search = $('#pac-input')
    , div_searchBox = $("#div_searchBox");
var map, marker, searchBox, resultMarker;
var mapLoaded = false;
$(function () {
    $("#bt_searchPlace").click(function () {
        if (searchBox != null) {
            searchBox.click();
        }
    });
    $(window).on('resize', function () {
        mapConvasFit();
    });
});

function loadGoogleMapApi(apiKey, callback) {
    clearMap();
    div_mapBody.show();
    if (!mapLoaded) {
        $.getScript("https://maps.googleapis.com/maps/api/js?libraries=places&callback=mapInit&key=" + apiKey).done(function (evt) {
            mapLoaded = true;
            callback();
        }).fail(function (evt) {});
    }
    else {
        callback();
    }
}

function mapConvasFit() {
    if (div_mapBody.css('display') != 'none') {
        console.log("mapConvasFit");
        $("#div_mapCanvas").height(window.innerHeight - $("#div_toolbar").height() - 68);
    }
}

function mapInit() {
    console.log("mapInit");
    mapConvasFit();
    var mapOptions = {
        center: new google.maps.LatLng(25.047829, 121.517056)
        , zoom: 17
    };
    map = new google.maps.Map(document.getElementById('div_mapCanvas'), mapOptions);
    google.maps.event.addListener(map, 'click', onMapClick);
    var input_search = document.getElementById('pac-input');
    searchBox = new google.maps.places.SearchBox(input_search);
    var search_wrapper = document.getElementById('div_searchBox');
    map.controls[google.maps.ControlPosition.TOP_LEFT].push(search_wrapper);
    div_searchBox.hover(
            // fade background div out when cursor enters, 
            function () {
                $(".background", this).stop().animate({
                    opacity: 1
                });
            }, // fade back in when cursor leaves
            function () {
                $(".background", this).stop().animate({
                    opacity: 0
                });
            })
        // allow positioning child div relative to parent
        .css('position', 'relative')
        // create and append background div 
        // (initially visible, obscuring parent's background)
        .append($("<div>").attr('class', 'background').css({
            backgroundColor: 'white'
            , position: 'absolute'
            , top: 0
            , left: 0
            , zIndex: -1
            , width: div_searchBox.width()
            , height: div_searchBox.height()
        }));
    $(".background", div_searchBox).stop().animate({
        opacity: 0
    });
    google.maps.event.addListener(searchBox, 'places_changed', onSearchResult);
}

function onMapClick(event) {
    var lat = event.latLng.lat();
    var lng = event.latLng.lng();
    tv_position.html(lat + ", " + lng);
    if (marker == null) {
        marker = new google.maps.Marker({
            position: event.latLng
            , map: map
            , title: 'Selected Position'
        });
    }
    else {
        marker.setPosition(event.latLng);
    }
    sendPosition(event.latLng);
}

function putResultMarker(lat, lng) {
    console.log("put result marker: lat=" + lat + ", lng=" + lng);
    var latlng = new google.maps.LatLng(lat, lng);
    if (resultMarker == null) {
        resultMarker = new google.maps.Marker({
            position: latlng
            , map: map
            , title: 'Search Result Position'
            , icon: 'http://maps.google.com/mapfiles/ms/icons/green-dot.png'
        });
    }
    else {
        resultMarker.setPosition(latlng);
    }
}

function onSearchResult() {
    var places = searchBox.getPlaces();
    if (places.length > 0) {
        tv_search.val("");
        var geo = places[0].geometry.location;
        var lat = geo.lat();
        var lng = geo.lng();
        map.panTo(new google.maps.LatLng(lat, lng));
        putResultMarker(lat, lng);
    }
}

function sendPosition(latLng) {
    sendMsg("geo:" + latLng.lat() + "," + latLng.lng());
}

function clearMap() {
    tv_position.html("Please click map to send position to your device");
    if (marker != null) {
        marker.setMap(null);
        marker = null;
    }
}