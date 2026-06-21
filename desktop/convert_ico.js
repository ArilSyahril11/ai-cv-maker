const pngToIco = require('png-to-ico');
const fs = require('fs');
pngToIco('c:/Users/Hype 7/Downloads/ai-cv-maker/assets/logoaicvmaker.png')
  .then(buf => {
    fs.writeFileSync('c:/Users/Hype 7/Downloads/ai-cv-maker/desktop/build/icon.ico', buf);
    console.log('Success');
  })
  .catch(console.error);
