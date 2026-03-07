package pl.polsl.bland.webapp.view;

final class SchematicPreviewFactory {

    private SchematicPreviewFactory() {
    }

    static String render() {
        return """
                <div class="sheet-note">Tryb arkusza: schemat ideowy / jednostki: SI</div>
                <svg class="sheet" viewBox="0 0 1280 860" role="img" aria-label="Schemat obwodu RLC">
                  <defs>
                    <pattern id="smallGrid" width="16" height="16" patternUnits="userSpaceOnUse">
                      <path d="M 16 0 L 0 0 0 16" fill="none" stroke="#e7edf3" stroke-width="1"></path>
                    </pattern>
                    <pattern id="largeGrid" width="80" height="80" patternUnits="userSpaceOnUse">
                      <rect width="80" height="80" fill="url(#smallGrid)"></rect>
                      <path d="M 80 0 L 0 0 0 80" fill="none" stroke="#d8e1ea" stroke-width="1"></path>
                    </pattern>
                  </defs>

                  <rect class="sheet-frame" x="10" y="10" width="1260" height="840" rx="1"></rect>
                  <rect x="36" y="36" width="1208" height="770" fill="url(#largeGrid)" stroke="#c7d0db" stroke-width="1.1"></rect>
                  <rect class="frame-border" x="36" y="36" width="1208" height="770"></rect>

                  <g class="title-block">
                    <rect x="852" y="720" width="392" height="86"></rect>
                    <line x1="852" y1="748" x2="1244" y2="748"></line>
                    <line x1="852" y1="776" x2="1244" y2="776"></line>
                    <line x1="1118" y1="748" x2="1118" y2="806"></line>
                    <line x1="1178" y1="776" x2="1178" y2="806"></line>
                    <text x="866" y="738">Projekt: Filtr RLC - ćwiczenie 04</text>
                    <text x="866" y="766">Autor: Laboratorium EE / grupa A2</text>
                    <text x="1132" y="766">Arkusz: 1 / 1</text>
                    <text x="866" y="794">Tryb: schemat ideowy</text>
                    <text x="1190" y="794">Skala: 1:1</text>
                  </g>

                  <g>
                    <path class="wire" d="M 184 240 L 292 240"></path>
                    <path class="wire" d="M 468 240 L 612 240"></path>
                    <path class="wire" d="M 816 240 L 940 240"></path>
                    <path class="wire" d="M 940 384 L 940 520 L 184 520"></path>
                    <path class="wire" d="M 184 434 L 184 520"></path>
                    <circle class="node-dot" cx="184" cy="240" r="4"></circle>
                    <circle class="node-dot" cx="468" cy="240" r="4"></circle>
                    <circle class="node-dot" cx="612" cy="240" r="4"></circle>
                    <circle class="node-dot" cx="816" cy="240" r="4"></circle>
                    <circle class="node-dot" cx="940" cy="240" r="4"></circle>
                    <circle class="node-dot" cx="184" cy="520" r="4"></circle>
                    <circle class="node-dot" cx="940" cy="520" r="4"></circle>
                    <text class="node-label" x="168" y="222">IN</text>
                    <text class="node-label" x="448" y="222">N001</text>
                    <text class="node-label" x="594" y="222">N002</text>
                    <text class="node-label" x="919" y="222">N003</text>
                  </g>

                  <g class="schematic-part is-selected">
                    <rect class="selection-halo" x="270" y="192" width="220" height="94" rx="3"></rect>
                    <path class="component-wire" d="M 292 240 L 308 240"></path>
                    <path class="component-shape no-fill" d="M 308 240 L 328 220 L 348 260 L 368 220 L 388 260 L 408 220 L 428 260 L 448 220 L 468 240"></path>
                    <text class="component-label" x="368" y="196">R1</text>
                    <text class="component-value" x="344" y="286">120 Ohm</text>
                  </g>

                  <g class="schematic-part">
                    <rect class="selection-halo" x="590" y="192" width="248" height="94" rx="3"></rect>
                    <path class="component-wire" d="M 612 240 L 630 240"></path>
                    <path class="component-shape no-fill" d="M 630 240
                      c 10 -20 30 -20 40 0
                      c 10 -20 30 -20 40 0
                      c 10 -20 30 -20 40 0
                      c 10 -20 30 -20 40 0"></path>
                    <path class="component-wire" d="M 790 240 L 816 240"></path>
                    <text class="component-label" x="706" y="196">L1</text>
                    <text class="component-value" x="688" y="286">22 mH</text>
                  </g>

                  <g class="schematic-part">
                    <rect class="selection-halo" x="892" y="194" width="96" height="234" rx="3"></rect>
                    <path class="component-wire" d="M 940 240 L 940 286"></path>
                    <path class="component-shape no-fill" d="M 920 286 L 960 286"></path>
                    <path class="component-shape no-fill" d="M 920 336 L 960 336"></path>
                    <path class="component-wire" d="M 940 336 L 940 384"></path>
                    <text class="component-label" x="972" y="312">C1</text>
                    <text class="component-value" x="972" y="330">4,7 uF</text>
                  </g>

                  <g class="schematic-part">
                    <rect class="selection-halo" x="132" y="194" width="104" height="272" rx="3"></rect>
                    <path class="component-wire" d="M 184 240 L 184 286"></path>
                    <circle class="component-shape no-fill" cx="184" cy="360" r="74"></circle>
                    <path class="component-shape no-fill" d="M 184 316 L 184 338"></path>
                    <path class="component-shape no-fill" d="M 173 327 L 195 327"></path>
                    <path class="component-shape no-fill" d="M 171 393 L 197 393"></path>
                    <path class="component-shape no-fill" d="M 150 360
                      C 161 345 173 345 184 360
                      C 195 375 207 375 218 360"></path>
                    <path class="component-wire" d="M 184 434 L 184 520"></path>
                    <text class="component-label" x="152" y="196">V1</text>
                    <text class="component-value" x="110" y="474">SIN(0 5 1k)</text>
                  </g>

                  <g class="schematic-part">
                    <rect class="selection-halo" x="138" y="514" width="96" height="80" rx="3"></rect>
                    <path class="component-wire" d="M 184 520 L 184 542"></path>
                    <path class="component-shape no-fill" d="M 156 542 L 212 542"></path>
                    <path class="component-shape no-fill" d="M 164 552 L 204 552"></path>
                    <path class="component-shape no-fill" d="M 172 562 L 196 562"></path>
                    <text class="component-label" x="148" y="590">GND</text>
                    <text class="component-value" x="146" y="606">węzeł odniesienia</text>
                  </g>

                  <text class="small-note" x="84" y="82">Edytor obwodu: tor RLC w konfiguracji szeregowej z kondensatorem do masy.</text>
                  <text class="small-note" x="84" y="100">Zaznaczenie: kliknij element. Wyniki po symulacji zależne od wybranego śladu.</text>
                </svg>
                """;
    }
}
