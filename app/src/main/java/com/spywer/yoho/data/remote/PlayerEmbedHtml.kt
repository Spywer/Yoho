package com.spywer.yoho.data.remote

/**
 * HTML-обёртки как на сайте: iframe-host + Vibix (rendex/ins) + CVH.
 * Грузим через loadDataWithBaseURL(reyoho.com).
 */
object PlayerEmbedHtml {
    private const val SHELL_CSS =
        "html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden}" +
            "html,body{position:fixed;inset:0}" +
            "#root{position:absolute;inset:0;width:100%;height:100%}" +
            "iframe,video,ins,video-player{" +
            "position:absolute;inset:0;width:100%!important;height:100%!important;" +
            "border:0;display:block;background:#000}"

    private const val IFRAME_ALLOW =
        "autoplay; fullscreen; picture-in-picture; encrypted-media; storage-access"

    /**
     * Как на сайте: iframe с referrerpolicy=origin под parent reyoho.com.
     */
    fun iframe(src: String): String {
        val safe = src.escapeHtmlAttr()
        return """
            <!DOCTYPE html><html><head>
            <meta charset="utf-8"/>
            <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"/>
            <style>$SHELL_CSS</style>
            </head><body>
            <iframe
              src="$safe"
              title="Плеер"
              referrerpolicy="origin"
              allow="$IFRAME_ALLOW"
              allowfullscreen
            ></iframe>
            </body></html>
        """.trimIndent()
    }

    /**
     * Vibix на сайте: `ek.L()` = rendex-sdk → `<ins data-*>` → `rendexSDK.init()`.
     * Прямой kinescope URL без SDK даёт «контент ещё не добавлен».
     */
    fun vibix(publisherId: String, embedId: String, embedType: String): String {
        val pub = publisherId.escapeHtmlAttr()
        val id = embedId.escapeHtmlAttr()
        val type = embedType.escapeHtmlAttr()
        return """
            <!DOCTYPE html><html><head>
            <meta charset="utf-8"/>
            <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"/>
            <style>$SHELL_CSS</style>
            </head><body><div id="root"></div>
            <script>
            (function(){
              var host=document.getElementById('root');
              var RENDEX='https://graphicslab.io/sdk/v2/rendex-sdk.min.js';
              function styleHost(){
                host.querySelectorAll('ins').forEach(function(t){
                  t.style.display='block';
                  t.style.width='100%';
                  t.style.height='100%';
                  t.style.margin='0';
                  t.style.padding='0';
                });
                host.querySelectorAll('iframe').forEach(function(t){
                  t.removeAttribute('height');
                  t.style.width='100%';
                  t.style.height='100%';
                  t.style.border='0';
                  t.setAttribute('allow','$IFRAME_ALLOW');
                  t.setAttribute('referrerpolicy','origin');
                  t.setAttribute('allowfullscreen','');
                });
              }
              function mount(){
                host.innerHTML='';
                var e=document.createElement('ins');
                e.setAttribute('data-publisher-id','$pub');
                e.setAttribute('data-type','$type');
                e.setAttribute('data-id','$id');
                e.setAttribute('data-design','2');
                e.setAttribute('data-color1','#333333');
                e.setAttribute('data-color2','#666666');
                e.setAttribute('data-color3','#999999');
                e.setAttribute('data-color4','#CCCCCC');
                e.setAttribute('data-color5','#FFFFFF');
                e.style.cssText='display:block;width:100%;height:100%';
                host.appendChild(e);
                try{
                  if(window.rendexSDK&&typeof window.rendexSDK.init==='function') window.rendexSDK.init();
                  else if(window.RendexSDK&&typeof window.RendexSDK.init==='function') window.RendexSDK.init();
                }catch(err){}
                styleHost();
                setTimeout(styleHost,400);
                setTimeout(styleHost,1500);
                setTimeout(styleHost,3000);
              }
              function loadRendex(){
                if(document.querySelector('script[src^="'+RENDEX+'"]')){
                  mount();
                  return;
                }
                var s=document.createElement('script');
                s.async=true;
                s.src=RENDEX;
                s.onload=function(){ mount(); };
                s.onerror=function(){ host.textContent='Vibix: не удалось загрузить SDK'; };
                document.head.appendChild(s);
              }
              if(document.readyState==='complete') loadRendex();
              else window.addEventListener('load', loadRendex);
            })();
            </script>
            </body></html>
        """.trimIndent()
    }

    fun cvh(titleId: String, publisherId: String, aggregator: String = "kp"): String {
        val title = titleId.escapeHtmlAttr()
        val pub = publisherId.escapeHtmlAttr()
        val agg = aggregator.escapeHtmlAttr()
        return """
            <!DOCTYPE html><html><head>
            <meta charset="utf-8"/>
            <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"/>
            <style>$SHELL_CSS</style>
            <script>
            /* CVH frame/index.js требует URL.canParse (Chrome 120+); старый WebView — polyfill */
            if (typeof URL !== 'undefined' && typeof URL.canParse !== 'function') {
              URL.canParse = function(url, base) {
                try { new URL(url, base); return true; } catch (e) { return false; }
              };
            }
            </script>
            </head><body><div id="root" class="_7FrL7W_host">
            <video-player data-title-id="$title" data-publisher-id="$pub" data-aggregator="$agg"></video-player>
            </div>
            <script src="https://graphicslab.io/sdk/v2/rendex-sdk.min.js"></script>
            <script src="https://player.cdnvideohub.com/s2/stable/video-player.umd.js"></script>
            <script>
            (function(){
              function boot(){
                try{ if(window.rendexSDK&&rendexSDK.init) rendexSDK.init(); }catch(err){}
                try{ if(window.RendexSDK&&RendexSDK.init) RendexSDK.init(); }catch(err){}
                try{ if(window.VideoPlayer&&VideoPlayer.init) VideoPlayer.init(); }catch(err){}
              }
              if(document.readyState==='complete') boot();
              else window.addEventListener('load', boot);
              setTimeout(boot,400);
              setTimeout(boot,1200);
            })();
            </script>
            </body></html>
        """.trimIndent()
    }

    private fun String.escapeHtmlAttr(): String =
        replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
}
