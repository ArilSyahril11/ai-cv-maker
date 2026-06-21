const GEMINI_MODELS = [
  'gemini-flash-lite-latest',
  'gemini-flash-latest',
  'gemini-2.5-flash',
  'gemini-2.0-flash'
];

function getApiKey() {
  const key = process.env.GEMINI_API_KEY || process.env.GOOGLE_API_KEY || '';
  if (!key || key === 'MY_GEMINI_API_KEY' || key.toLowerCase().includes('placeholder')) {
    return null;
  }
  return key.trim();
}

function buildPrompt(type, originalText) {
  switch (type) {
    case 'summary':
      return `Anda adalah asisten penulisan resume profesional. Tulis ulang ringkasan profil profesional (about me) berikut agar terdengar sangat menarik bagi rekruter HR.\nGunakan bahasa yang profesional, percaya diri, dan berorientasi pada pencapaian. Buat dalam 2-3 kalimat yang solid dan bermakna tinggi.\nTeks asli: "${originalText}"\nHasil dalam Bahasa Indonesia yang formal dan profesional:`;
    case 'experience':
      return `Anda adalah asisten penulisan resume profesional. Tulis ulang deskripsi pengalaman kerja berikut ke dalam poin-poin pencapaian profesional yang singkat, padat, dan berdampak tinggi.\nGunakan action verbs (kata kerja aksi), sebutkan metrik/kemungkinan dampak jika relevan, dan hindari kata-kata klise.\nTeks asli: "${originalText}"\nHasil dalam Bahasa Indonesia yang formal dan profesional (berupa 2-3 poin dengan format baris baru, tanpa karakter aneh):`;
    case 'education':
      return `Anda adalah asisten penulisan resume profesional. Ringkas atau tulis ulang deskripsi pendidikan berikut agar terdengar formal, rapi, dan relevan dengan industri kerja.\nTeks asli: "${originalText}"\nHasil dalam Bahasa Indonesia yang formal:`;
    default:
      return `Tulis ulang teks berikut agar terdengar lebih profesional dan menarik untuk CV/Resume kerja.\nTeks asli: "${originalText}"\nHasil dalam Bahasa Indonesia formal:`;
  }
}

async function callGemini(apiKey, body) {
  const headers = {
    'Content-Type': 'application/json',
    'x-goog-api-key': apiKey,
  };

  let lastError = '';

  for (const model of GEMINI_MODELS) {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`;
    try {
      const response = await fetch(url, {
        method: 'POST',
        headers,
        body: JSON.stringify(body),
      });

      if (response.ok) {
        return response.json();
      }

      lastError = await response.text();
      if (response.status === 404) continue;
      if (response.status === 401 || response.status === 403) {
        return { error: `Autentikasi gagal (${response.status}): Periksa kunci API Gemini Anda.` };
      }
    } catch (err) {
      lastError = err.message;
    }
  }

  return { error: `Gagal menghubungi server AI: ${lastError}` };
}

async function polishText(type, originalText, clientApiKey) {
  const apiKey = clientApiKey || getApiKey();
  if (!apiKey) {
    return 'Error: Kunci API Gemini tidak valid atau belum diatur. Buat file .env di folder root proyek dengan GEMINI_API_KEY=kunci_anda, atau atur di Pengaturan Aplikasi.';
  }

  const body = {
    contents: [{ parts: [{ text: buildPrompt(type, originalText) }] }],
    systemInstruction: {
      parts: [{ text: 'Anda adalah pakar HR dan penulis CV profesional. Tugas Anda adalah membantu menulis entri CV yang pendek, menarik, berdampak tinggi dan bebas kesalahan tata bahasa.' }],
    },
  };

  const data = await callGemini(apiKey, body);
  if (data.error) return data.error;

  const rewritten = data.candidates?.[0]?.content?.parts?.[0]?.text;
  return rewritten?.trim() || 'Gagal mendapatkan hasil pemolesan teks.';
}

async function analyzeCv(cvData, clientApiKey) {
  const apiKey = clientApiKey || getApiKey();
  if (!apiKey) {
    return { error: 'API key tidak ditemukan. Pastikan GEMINI_API_KEY sudah diatur di file .env, atau atur di Pengaturan Aplikasi.' };
  }

  const cvText = JSON.stringify(cvData, null, 2);

  const prompt = `Anda adalah pakar rekrutmen HR dan penulis CV profesional senior. Analisa data CV berikut secara mendalam dan kembalikan HANYA respons JSON valid (tanpa markdown, tanpa blok kode) dengan format berikut:

{
  "overallScore": <angka 0-100>,
  "grade": "<A/B/C/D>",
  "scoreLabel": "<label singkat, misal Sangat Baik>",
  "categories": {
    "kelengkapan": <0-100>,
    "pengalaman": <0-100>,
    "portfolio": <0-100>,
    "pendidikan": <0-100>,
    "keahlian": <0-100>,
    "kualitasTeks": <0-100>
  },
  "typos": [
    {
      "original": "<teks salah>",
      "correction": "<koreksi benar>",
      "context": "<konteks singkat>",
      "fieldPath": "<misal skills[0].name>",
      "severity": "error"
    }
  ],
  "anomalies": [
    {
      "text": "<teks anomali>",
      "context": "<lokasi>",
      "suggestion": "<saran perbaikan spesifik>",
      "severity": "warning"
    }
  ],
  "suggestions": [
    {
      "category": "<Pengalaman/Portfolio/Teks/Pendidikan/Keahlian>",
      "text": "<saran konkret>"
    }
  ],
  "strengths": ["<kelebihan 1>", "<kelebihan 2>"]
}

Data CV:
${cvText}

PENTING: Hanya kembalikan JSON mentah. Periksa typo umum seperti Ilustrator vs Illustrator, placeholder seperti (X) [X], dan teks tidak informatif.`;

  const body = {
    contents: [{ parts: [{ text: prompt }] }],
    generationConfig: { 
      temperature: 0.2,
      responseMimeType: "application/json"
    },
  };

  const data = await callGemini(apiKey, body);
  if (data.error) return { error: data.error };

  const raw = data.candidates?.[0]?.content?.parts?.[0]?.text || '';
  try {
    const cleaned = raw.replace(/^```json\s*/i, '').replace(/\s*```$/i, '').trim();
    return JSON.parse(cleaned);
  } catch {
    return { error: 'Gagal mem-parsing respons AI. Coba lagi.' };
  }
}

async function scanCvReference(base64Data, mimeType, clientApiKey) {
  const apiKey = clientApiKey || getApiKey();
  if (!apiKey) {
    return { error: 'API key tidak ditemukan. Pastikan GEMINI_API_KEY sudah diatur.' };
  }

  const prompt = `Anda adalah seorang ahli rekayasa Frontend (Tailwind CSS) dan Data Extractor profesional. Saya memberikan Anda referensi CV dalam bentuk file (gambar atau dokumen). 
Tugas Anda ada DUA:
1. Ekstrak SELURUH data teks dari referensi CV tersebut ke dalam format JSON yang terstruktur.
2. Tirukan desain, warna, tata letak, dan gaya CV tersebut ke dalam kode HTML yang diwarnai menggunakan kelas-kelas utilitas Tailwind CSS.

Anda HARUS mengembalikan SATU objek JSON mentah yang valid tanpa pembungkus Markdown, dengan struktur persis seperti ini:

{
  "cvData": {
    "personal": {
      "fullName": "<nama>",
      "title": "<pekerjaan/title>",
      "email": "<email>",
      "phone": "<telepon>",
      "location": "<lokasi>",
      "linkedin": "<linkedin>",
      "website": "<website/portfolio>",
      "summary": "<profil atau ringkasan pribadi>"
    },
    "experiences": [
      {
        "id": "exp_1",
        "company": "<perusahaan>",
        "title": "<jabatan>",
        "startDate": "<tanggal mulai>",
        "endDate": "<tanggal selesai>",
        "description": "<deskripsi pekerjaan menggunakan bullet point jika ada>"
      }
    ],
    "educations": [
      {
        "id": "edu_1",
        "institution": "<institusi>",
        "major": "<jurusan>",
        "startDate": "<tanggal mulai>",
        "endDate": "<tanggal selesai>"
      }
    ],
    "skills": [
      {
        "id": "skill_1",
        "name": "<nama skill>",
        "level": "<Pemula/Menengah/Mahir/Sangat Mahir>"
      }
    ],
    "languages": [
      {
        "id": "lang_1",
        "name": "<nama bahasa>",
        "proficiency": "<Tingkat kemahiran>"
      }
    ],
    "certifications": [],
    "organizations": [],
    "hobbies": []
  },
  "templateHtml": "KODE HTML TAILWIND ANDA DI SINI"
}

PANDUAN PEMBUATAN templateHtml:
- SANGAT KRITIS: Anda HARUS meniru 100% tata letak (layout), rasio grid/kolom, alignment, warna latar belakang (ekstrak HEX aktual), warna teks, tipografi (ukuran/ketebalan font), padding, dan margin dari gambar referensi menggunakan Tailwind CSS. Jangan gunakan layout generik!
- TERJEMAHKAN LABEL KE BAHASA INDONESIA: Jika gambar asli memiliki judul bagian (section) seperti "EDUCATION", "WORK EXPERIENCE", atau "SKILLS", Anda WAJIB mengubah teks statis tersebut ke dalam Bahasa Indonesia yang formal di templateHtml Anda (contoh: "RIWAYAT PENDIDIKAN", "PENGALAMAN KERJA", "KEAHLIAN"). 
- DILARANG KERAS menggunakan class responsif seperti \`md:\`, \`lg:\`, \`sm:\` atau \`xl:\`. Gunakan class langsung (misalnya \`grid-cols-2\`, BUKAN \`md:grid-cols-2\`).
- PASTIKAN MUAT 1 HALAMAN: Agar hasil CV tidak tumpah ke halaman 2, Anda WAJIB menggunakan ukuran font yang lebih kecil (misalnya \`text-[10px]\` atau \`text-xs\`) untuk isi konten, serta margin/padding yang rapat (misalnya \`gap-1\`, \`py-0.5\`, \`mb-1\`).
- FOTO PROFIL: Anda WAJIB memanggil blok variabel \`\${photoHtml}\` pada posisi letak foto profil berada (jangan membuat elemen img manual).
- Kode HTML harus dibungkus dalam SATU elemen kontainer.
- GUNAKAN SINTAKS INTERPOLASI javascript \`\${d.properti}\` UNTUK DATA PRIBADI (Contoh: \${d.personal.fullName}, \${d.personal.title}, \${d.personal.summary}).
- UNTUK DATA ARRAY (experiences, educations, skills, dll): JANGAN gunakan logika JS (map/forEach)! Anda WAJIB menyertakan blok variabel berikut ke dalam templateHtml: \`\${experiencesHtml}\`, \`\${educationsHtml}\`, \`\${skillsHtml}\`, \`\${languagesHtml}\`, \`\${certsHtml}\`, \`\${orgsHtml}\`, \`\${hobbiesHtml}\`, dan \`\${contactSidebarHtml}\`.
- Letakkan variabel blok tersebut tepat di posisi tata letak yang sesuai dengan gambar. Jika di PDF ada bagian "Pengalaman Kerja", letakkan \`\${experiencesHtml}\` di div tersebut!
- SOSIAL MEDIA & LINK: Ekstrak seluruh kontak (email, hp, alamat, linkedin, portfolio) dan letakkan hanya di bagian \`personal\`. Variabel \`\${contactSidebarHtml}\` yang akan merendernya.
- Anda TIDAK boleh memakai karakter backtick (\`) di dalam nilai templateHtml.
- Pastikan escape quote JSON valid. HANYA KEMBALIKAN JSON MENTAH!`;

  const body = {
    contents: [
      {
        parts: [
          { text: prompt },
          {
            inlineData: {
              mimeType: mimeType,
              data: base64Data
            }
          }
        ]
      }
    ],
    generationConfig: { 
      temperature: 0.2,
      responseMimeType: "application/json"
    },
  };

  const data = await callGemini(apiKey, body);
  if (data.error) return { error: data.error };

  const raw = data.candidates?.[0]?.content?.parts?.[0]?.text || '';
  try {
    const cleaned = raw.replace(/^```json\s*/i, '').replace(/\s*```$/i, '').trim();
    return JSON.parse(cleaned);
  } catch (err) {
    return { error: 'Gagal mem-parsing respons AI. Mungkin formatnya salah.', details: err.message };
  }
}

module.exports = { polishText, analyzeCv, scanCvReference, getApiKey };
