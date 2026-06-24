# AI CV Maker - Release Notes v1.0.1

Versi ini merupakan perbaikan (hotfix) stabilitas pada fitur utama **Scan CV AI** dan performa integrasi sistem *Frontend* dengan *Desktop/Android Bridge*. Versi ini dirilis setelah menemukan beberapa kendala *layout* dan penyimpanan ketika pengguna menggunakan opsi *Scan CV AI*.

## 🐛 Bug & Error yang Ditemukan
1. **Bug Template Custom Hilang Saat Aplikasi Di-restart:**
   *Dampak:* Pengguna yang sudah pernah melakukan *Scan CV AI* akan kehilangan opsi desain kustom mereka jika aplikasi dimuat ulang.
   *Penyebab:* Fungsi pemuatan daftar template custom tidak dipanggil pada siklus hidup `DOMContentLoaded`.
2. **Bug "Tombol Hapus" Menghilang Secara Gaib:**
   *Dampak:* Ikon "tong sampah merah" untuk menghapus desain AI hilang dari layar.
   *Penyebab:* Logika untuk mendeteksi *template* AI berjalan lebih dulu (*race condition*) dibandingkan dengan proses pembaruan menu *dropdown*.
3. **Error Pemrosesan AI Template ("Malah tidak muncul") pada Versi Desktop:**
   *Dampak:* Scan CV di aplikasi berjalan tanpa menghasilkan layout desain baru, malah terjebak di mode layout biasa.
   *Penyebab:* Perbedaan susunan skema JSON yang diwajibkan oleh *prompt* `gemini.js` versi Desktop (menggunakan `templateHtml` di *root*) dengan yang diharapkan oleh `index.html` (mencari `templateStyles.templateHtml`).
4. **Crash `ReferenceError: rounded is not defined` pada Render Engine:**
   *Dampak:* Jika pengguna mengganti bentuk foto (Kotak/Bulat) pada layout hasil buatan AI, muncul kotak merah bertuliskan error gagal render.
   *Penyebab:* AI kadang-kadang menyematkan sintaks `${rounded}`, `${sizeClass}`, dan `${justify}` secara harfiah (menghalusinasi struktur sintaks di luar *template string*) yang gagal dievaluasi oleh sistem pembuat fungsi lokal (`new Function`).
5. **Layout Foto Raksasa (Stretching Issue):**
   *Dampak:* Desain buatan AI meregangkan foto pengguna setinggi pilar atau melampaui lebar halaman.
   *Penyebab:* Tag `<img>` di dalam foto blok awalnya di-*set* paksa untuk menggunakan `w-full h-full`, dan ketika diletakkan oleh AI ke sebuah *grid column*, foto itu meregang menutupi seluruh kolom.

## 🛠️ Fix & Perbaikan yang Diterapkan
- **Sinkronisasi Otomatis:** Memasukkan perintah `renderCustomAiOptions()` ke siklus inisiasi sehingga *template* AI yang disimpan via `localStorage` selalu bertahan (*persistent*).
- **Penempatan Ulang Kondisional:** Fungsi `toggleDeleteTemplateButton()` dipindah untuk dieksekusi persis setelah elemen DOM dari menu dropdown diperbarui (*post-assignment*), baik dari *openEditor* maupun *handleScanCv*.
- **Kompatibilitas Ganda JSON:** Kode *frontend* sekarang bisa mendeteksi `aiData.templateStyles` maupun struktur datar `aiData.templateHtml`, sehingga fitur ekstrak akan sukses 100% dari Platform manapun (Web, Desktop, atau Android).
- **Pengamanan Mesin Render:** Menginjeksikan semua variabel lokal terkait foto (`rounded`, `sizeClass`, `justify`) ke dalam deklarasi evaluator `new Function()`, sehingga jika AI "kreatif" menciptakan sintaks acak untuk class Tailwind tersebut, sistem tidak akan *crash*.
- **Pagar Batas Foto Profil:** Membungkus elemen blok *photoHtml* ke dalam *container* pengaman khusus berdasar variabel ukuran (`${sizeClass}`) untuk menghentikan efek *stretching* tak terbatas akibat CSS Grid.

Status Keseluruhan Sistem: **STABIL ✅**
