const fs = require('fs');
const path = require('path');
const file = 'c:/Users/Hype 7/Downloads/ai-cv-maker/app/src/main/assets/index.html';
let content = fs.readFileSync(file, 'utf8');

// Replace top head CDNs
content = content.replace(
    /<!-- Tailwind CSS -->[\s\S]*?<script src="https:\/\/cdnjs\.cloudflare\.com\/ajax\/libs\/cropperjs\/1\.6\.1\/cropper\.min\.js"><\/script>/,
<!-- Offline Local Assets (Vendor) -->
    <script src="./vendor/tailwindcss.js"></script>
    <link href="./vendor/google-fonts.css" rel="stylesheet">
    <link rel="stylesheet" href="./vendor/all.min.css">
    <link rel="stylesheet" href="./vendor/cropper.min.css">
    <script src="./vendor/cropper.min.js"></script>
);

// Replace iframe buildStandaloneHtml CDNs & Inject basePath
content = content.replace(
    /return \<!DOCTYPE html>[\s\S]*?<link rel="stylesheet" href="https:\/\/cdnjs\.cloudflare\.com\/ajax\/libs\/font-awesome\/6\.4\.0\/css\/all\.min\.css">/,
const basePath = window.location.href.substring(0, window.location.href.lastIndexOf('/'));
            return \<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<title>CV</title>
<style>\</style>
<link href="\/vendor/google-fonts.css" rel="stylesheet">
<link rel="stylesheet" href="\/vendor/all.min.css">
);

fs.writeFileSync(file, content, 'utf8');
console.log('Update success');
