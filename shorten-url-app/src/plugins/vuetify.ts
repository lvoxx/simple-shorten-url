import { createVuetify } from "vuetify";
import { VFileUpload } from "vuetify/labs/VFileUpload";

const vuetify = createVuetify({
  theme: {
    defaultTheme: "dark",
    //
  },
  icons: {
    defaultSet: "mdi",
  },
  components: {
    VFileUpload,
  },
});

export default vuetify;
