# phrame - Photo frame application backed by Picasa Web Albums 

Software to run on Raspberry Pi based photo frame.

## Client install

    $ sudo apt-get install -y python-pip
    $ sudo pip install websocket-client

## Links

http://simoneast.org/digital-picture-frames/
https://maurits.wordpress.com/2010/10/27/using-google-data-apis-with-clojure/
https://developers.google.com/gdata/articles/java_client_lib
https://developers.google.com/picasa-web/docs/2.0/developers_guide_java#ListAlbums
https://developers.google.com/gdata/javadoc/com/google/gdata/data/photos/package-summary
http://raspberrypi.stackexchange.com/questions/8922/how-do-i-display-images-without-starting-x11
http://stackoverflow.com/questions/11390596/how-to-display-image-in-pygame
http://stackoverflow.com/questions/20002242/how-to-scale-images-to-screen-size-in-pygame

### OAuth2 with Google + PicasaWeb

http://holtstrom.com/michael/blog/post/522/Google-OAuth2-with-PicasaWeb.html
https://console.developers.google.com/project/netzhansa/apiui/credential?authuser=0#
https://accounts.google.com/o/oauth2/auth?scope=https://picasaweb.google.com/data/+https://www.googleapis.com/auth/userinfo.email&response_type=code&access_type=offline&redirect_uri=https://netzhansa.com/oauth2callback&approval_prompt=force&client_id=60990417096-kmhdpmjat3g8ip969klhvobphknotj7q.apps.googleusercontent.com

### Misc helpful links
https://stackoverflow.com/questions/3971841/how-to-resize-images-proportionally-keeping-the-aspect-ratio/14731922#14731922

### Datomic & Clojure related
https://github.com/cldwalker/datomic-free

## License

Copyright © 2015 Hans Hübner

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
