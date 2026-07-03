<script setup>
defineProps({
  type: {
    type: String,
    required: true,
  },
})
</script>

<template>
  <svg
    v-if="type === 'archive'"
    class="visual visual-archive"
    viewBox="0 0 330 230"
    role="img"
    aria-label="鹦鹉档案插画"
  >
    <ellipse cx="130" cy="205" rx="82" ry="14" fill="#d8c7c1" opacity=".28" />
    <path d="M98 77c15-43 72-45 97-8 28 42 3 105-45 115-43 8-77-17-79-58-1-16 11-34 27-49Z" fill="#f8f8f1" />
    <path d="M62 137c-22 28-41 57-37 66 8 16 67-20 92-52L93 125c-10 1-21 4-31 12Z" fill="#f36a21" />
    <path d="M185 83c38 14 66 42 91 79-31 1-72-10-108-36Z" fill="#ec6a22" />
    <path d="M178 91c30 9 55 27 80 52-29-6-56-16-88-40Z" fill="#f07f26" />
    <path d="M111 62c-28 4-48 23-55 54 31 7 59-6 75-32Z" fill="#2f5225" />
    <path d="M127 92c20-18 52-28 71-19 2 28-21 60-53 66-27 5-45-12-18-47Z" fill="#f47b2c" />
    <path d="M93 84c17-23 56-26 79-6-18 19-47 24-79 6Z" fill="#ffffff" />
    <path d="M160 103c7 17 7 34-2 49-23 4-45-1-59-14 3-22 14-36 33-45Z" fill="#222222" />
    <circle cx="151" cy="78" r="10" fill="#101010" />
    <circle cx="154" cy="74" r="3.5" fill="#fff" />
    <path d="M169 70c21-4 36 1 45 14-13 10-27 14-44 11Z" fill="#f3722c" />
    <path d="M167 69c15-13 31-14 46-5-8 11-21 17-40 19Z" fill="#ffffff" />
    <path d="M188 93c23 8 42 23 58 42" fill="none" stroke="#345c2a" stroke-width="5" stroke-linecap="round" stroke-dasharray="16 13" />
    <path d="M49 174c21-4 38-12 55-29" fill="none" stroke="#245321" stroke-width="5" stroke-linecap="round" stroke-dasharray="18 14" />
    <path d="M115 178l-6 24M149 178l8 24" stroke="#a4652a" stroke-width="4" stroke-linecap="round" />
    <path d="M94 203c17-2 29 0 37 6M146 204c18-3 31-2 41 4" stroke="#a4652a" stroke-width="4" stroke-linecap="round" />
    <path d="M173 125h106c10 0 18 8 18 18v62c0 10-8 18-18 18H129c-10 0-18-8-18-18v-42c0-21 17-38 38-38h7v-18c0-7 6-13 13-13h62c8 0 14 6 14 14v17h-72Z" fill="#ffd25f" />
    <path d="M157 118h60c8 0 15 7 15 15v6h-90v-6c0-8 7-15 15-15Z" fill="#f8f8f2" />
    <rect x="233" y="163" width="48" height="25" rx="4" fill="#fffaf1" />
  </svg>

  <svg
    v-else-if="type === 'growth'"
    class="visual visual-growth"
    viewBox="0 0 360 360"
    role="img"
    aria-label="成长报告插画"
  >
    <ellipse cx="174" cy="322" rx="132" ry="18" fill="#d9c6bd" opacity=".22" />
    <path d="M46 261c45-31 100-47 160-44 47 3 91 18 120 44-46 41-214 52-280 0Z" fill="none" stroke="#bb8a4d" stroke-width="18" stroke-linecap="round" />
    <path d="M42 242c52 37 216 43 284 2M61 214c67 41 177 46 247 2M86 195c55 34 138 38 198 5M72 285c63-24 170-30 241-5" fill="none" stroke="#c99b61" stroke-width="11" stroke-linecap="round" />
    <path d="M60 235c-14-33-9-72 7-98M100 217c-2-35 7-68 27-98M148 211c6-33 21-61 48-84M205 213c19-35 43-57 75-67M263 225c25-23 49-34 78-37" fill="none" stroke="#b98242" stroke-width="8" stroke-linecap="round" />
    <ellipse cx="129" cy="252" rx="40" ry="54" fill="#cce7f5" transform="rotate(-12 129 252)" />
    <ellipse cx="190" cy="247" rx="37" ry="55" fill="#ffe7a6" transform="rotate(8 190 247)" />
    <ellipse cx="251" cy="253" rx="39" ry="51" fill="#fffaf1" transform="rotate(16 251 253)" />
    <circle cx="119" cy="239" r="4" fill="#9ecbdd" opacity=".7" />
    <circle cx="145" cy="270" r="3" fill="#9ecbdd" opacity=".7" />
    <circle cx="268" cy="239" r="3" fill="#dfd4c5" />
    <circle cx="240" cy="269" r="3" fill="#dfd4c5" />
    <path d="M222 181c9-37 49-48 70-22 22 28 1 70-34 73-27 2-42-18-36-51Z" fill="#ffec85" />
    <path d="M207 196c18 10 40 12 59 4-8 24-35 35-59 21Z" fill="#ffd153" />
    <path d="M280 178c13 0 24 7 32 19-10 6-20 7-31 3Z" fill="#f47b59" />
    <circle cx="271" cy="169" r="6" fill="#27231f" />
    <path d="M235 232l-5 16M259 232l7 17" stroke="#e28a42" stroke-width="4" stroke-linecap="round" />
    <path d="M309 213c22-12 31-31 35-58" fill="none" stroke="#6c9837" stroke-width="8" stroke-linecap="round" />
    <path d="M341 159c-20-4-34 4-42 24 20 4 34-4 42-24Z" fill="#79ad42" />
    <path d="M309 197c-18-4-29 2-37 19 17 5 31-2 37-19Z" fill="#7eb644" />
    <path d="M75 286c-18 2-32 9-44 25M318 284c18 0 32 8 44 25" stroke="#8aa53f" stroke-width="7" stroke-linecap="round" />
    <path d="M47 307c21 4 34-3 40-22-20-5-34 2-40 22ZM326 306c-20 4-34-3-40-22 20-5 34 2 40 22Z" fill="#91b347" />
  </svg>

  <svg
    v-else-if="type === 'settings'"
    class="visual visual-settings"
    viewBox="0 0 320 300"
    role="img"
    aria-label="用户设置插画"
  >
    <ellipse cx="181" cy="270" rx="84" ry="12" fill="#d8c7bd" opacity=".24" />
    <path d="M177 67c10-35 27-57 60-67-9 43-24 72-52 89Z" fill="#ffe884" />
    <path d="M194 76c18-31 40-48 75-49-19 38-43 60-78 72Z" fill="#ffe884" />
    <path d="M147 115c8-43 48-69 90-52 56 23 59 116 2 156-43 30-103 18-119-31-9-28 0-54 27-73Z" fill="#fff4bd" />
    <path d="M226 96c44 14 74 56 71 107-26 15-66 8-88-20-27-35-20-73 17-87Z" fill="#aaa9ac" />
    <path d="M137 138c-24 9-37 22-45 42 22 13 51 2 65-25Z" fill="#f08061" />
    <circle cx="191" cy="122" r="8" fill="#1d1b1b" />
    <circle cx="193" cy="119" r="3" fill="#fff" />
    <circle cx="155" cy="160" r="20" fill="#ff9d4f" />
    <path d="M167 229l-6 30M208 229l12 29" stroke="#e4947b" stroke-width="6" stroke-linecap="round" />
    <path d="M147 262c19-3 32-1 42 6M206 262c21-4 37-2 48 5" stroke="#e4947b" stroke-width="5" stroke-linecap="round" />
    <g transform="translate(47 118)">
      <path d="M83 0l15 15 23-4 8 26 20 11-10 26 7 23-25 13-12 22-28-5-22 10-17-20-24-7-1-28L0 64l14-23-2-25 27-8L57-10Z" fill="#78983e" />
      <circle cx="74" cy="63" r="50" fill="#90ad52" />
      <circle cx="74" cy="63" r="27" fill="#f4f6dc" />
      <circle cx="74" cy="63" r="18" fill="#78983e" />
    </g>
  </svg>

  <svg
    v-else-if="type === 'medical'"
    class="visual visual-medical"
    viewBox="0 0 330 330"
    role="img"
    aria-label="医疗助手插画"
  >
    <ellipse cx="174" cy="304" rx="82" ry="13" fill="#d8b9a4" opacity=".32" />
    <path d="M146 64c11-34 31-55 63-64-9 42-25 69-54 86Z" fill="#ffe777" />
    <path d="M124 119c7-45 52-72 96-53 57 25 63 124 4 170-45 35-111 22-131-30-12-32-1-65 31-87Z" fill="#fff3a9" />
    <path d="M209 99c43 15 72 59 68 113-27 17-70 8-92-23-28-38-20-76 24-90Z" fill="#9c9ea1" />
    <path d="M112 147c-23 9-37 24-43 44 23 12 51 1 65-27Z" fill="#f18b68" />
    <circle cx="172" cy="127" r="8" fill="#1d1b1b" />
    <circle cx="174" cy="124" r="3" fill="#fff" />
    <circle cx="134" cy="166" r="19" fill="#ff9c4b" />
    <path d="M109 196c18 29 56 42 91 25 15 16 30 29 56 35-28 31-118 37-159-17-9-13-12-28 12-43Z" fill="#ffffff" />
    <path d="M84 114c25-17 63-22 103-6l5 35c-48 10-88 3-119-18 1-4 5-8 11-11Z" fill="#ffffff" />
    <path d="M127 110v34M110 127h34" stroke="#f25e3d" stroke-width="10" stroke-linecap="round" />
    <path d="M141 251l-5 32M187 250l13 32" stroke="#ea967d" stroke-width="6" stroke-linecap="round" />
    <path d="M122 287c20-3 35 0 45 7M185 287c23-4 40-1 53 6" stroke="#ea967d" stroke-width="5" stroke-linecap="round" />
    <path d="M86 197c-39 46-35 89 11 88 45-1 55-79 17-91" fill="none" stroke="#4f5862" stroke-width="8" stroke-linecap="round" />
    <path d="M210 193c38 8 50 70 16 82-30 10-55-28-31-55" fill="none" stroke="#4f5862" stroke-width="8" stroke-linecap="round" />
    <circle cx="194" cy="220" r="11" fill="#66717b" />
    <circle cx="84" cy="198" r="13" fill="#66717b" />
  </svg>

  <svg
    v-else-if="type === 'ledger'"
    class="visual visual-ledger"
    viewBox="0 0 420 240"
    role="img"
    aria-label="记账本插画"
  >
    <ellipse cx="199" cy="218" rx="103" ry="12" fill="#b4c6cc" opacity=".25" />
    <path d="M119 86c9-46 55-78 99-66 55 14 79 79 46 133-33 56-111 68-152 29-27-26-24-65 7-96Z" fill="#fdfdf5" />
    <path d="M112 123c-49 36-85 75-93 101 48-6 97-36 139-78Z" fill="#72b4d3" />
    <path d="M153 120c-29 12-52 35-68 68 35 4 71-12 99-45Z" fill="#83c5df" />
    <path d="M214 78c21-10 38-9 53 3-10 17-24 25-43 27Z" fill="#61acd3" />
    <path d="M128 73c32-29 77-26 108 1-27 23-71 23-108-1Z" fill="#fff" />
    <path d="M157 106c28 12 59 10 84-6 3 25-15 48-44 50-25 1-43-15-40-44Z" fill="#ffffff" />
    <circle cx="177" cy="68" r="8" fill="#24201f" />
    <circle cx="180" cy="65" r="3" fill="#fff" />
    <circle cx="212" cy="66" r="7" fill="#24201f" />
    <circle cx="214" cy="63" r="3" fill="#fff" />
    <circle cx="178" cy="91" r="5" fill="#1f1f1f" opacity=".45" />
    <circle cx="218" cy="94" r="5" fill="#1f1f1f" opacity=".45" />
    <path d="M130 183l-6 24M169 182l9 25" stroke="#e99556" stroke-width="5" stroke-linecap="round" />
    <path d="M112 210c19-3 33-1 45 5M166 210c20-3 35-1 47 5" stroke="#e99556" stroke-width="4" stroke-linecap="round" />
    <rect x="183" y="110" width="150" height="92" rx="13" fill="#84a55b" />
    <path d="M199 124h111c6 0 11 5 11 11v54H199c-8 0-14-6-14-14v-37c0-8 6-14 14-14Z" fill="#78984d" />
    <path d="M207 139h84M207 160h60M205 184h86" stroke="#b7ca82" stroke-width="4" stroke-linecap="round" stroke-dasharray="6 8" />
    <circle cx="308" cy="157" r="11" fill="#f8dd95" />
    <circle cx="358" cy="184" r="34" fill="#f2b94e" />
    <circle cx="358" cy="184" r="25" fill="#ffd46b" />
    <path d="M360 165v39M349 175c3-8 22-9 22 3 0 16-25 8-25 21 0 11 22 11 26 2" stroke="#bd7b2d" stroke-width="5" stroke-linecap="round" />
  </svg>

  <svg
    v-else-if="type === 'handbook'"
    class="visual visual-handbook"
    viewBox="0 0 380 260"
    role="img"
    aria-label="饲养手册插画"
  >
    <ellipse cx="204" cy="230" rx="102" ry="14" fill="#c6c2ce" opacity=".28" />
    <path d="M240 43c11-33 31-53 62-61-7 41-24 66-53 83Z" fill="#ffe779" />
    <path d="M226 87c7-44 50-70 92-52 56 24 62 119 4 162-44 34-108 20-126-30-12-31-1-62 30-80Z" fill="#fff2a6" />
    <path d="M309 67c42 14 70 55 66 107-26 16-68 7-89-22-27-37-19-72 23-85Z" fill="#aaa9ac" />
    <path d="M214 111c-23 9-36 22-43 42 23 12 50 1 64-26Z" fill="#f18a67" />
    <circle cx="270" cy="96" r="8" fill="#1f1f1f" />
    <circle cx="272" cy="93" r="3" fill="#fff" />
    <circle cx="231" cy="135" r="19" fill="#ff9d4f" />
    <path d="M243 194l-6 29M286 194l11 29" stroke="#e9957d" stroke-width="6" stroke-linecap="round" />
    <path d="M226 226c20-3 34-1 45 6M286 226c22-3 38-1 50 5" stroke="#e9957d" stroke-width="5" stroke-linecap="round" />
    <path d="M33 102c48 8 94 23 139 46v96c-44-25-89-40-139-48Z" fill="#667fae" />
    <path d="M172 148c49-23 97-33 145-27v95c-49-5-96 4-145 28Z" fill="#879bc6" />
    <path d="M43 113c42 8 81 21 119 40v76c-39-19-78-32-119-39Z" fill="#556f9c" />
    <path d="M184 154c40-17 79-25 118-22v73c-40-2-79 5-118 22Z" fill="#758bb9" />
    <path d="M189 160h66" stroke="#e8d7ad" stroke-width="7" stroke-linecap="round" />
    <path d="M94 150h42" stroke="#e8d7ad" stroke-width="6" stroke-linecap="round" />
    <circle cx="239" cy="196" r="26" fill="#ffe2a3" />
    <circle cx="230" cy="194" r="6" fill="#d69028" />
    <circle cx="246" cy="190" r="6" fill="#d69028" />
    <circle cx="243" cy="204" r="6" fill="#d69028" />
  </svg>

  <svg
    v-else-if="type === 'avatar-orange'"
    class="visual visual-avatar"
    viewBox="0 0 130 130"
    role="img"
    aria-label="当前鹦鹉头像"
  >
    <circle cx="65" cy="65" r="61" fill="#fff6df" />
    <ellipse cx="62" cy="111" rx="45" ry="7" fill="#d7b79d" opacity=".28" />
    <path d="M42 47c10-26 44-29 60-5 17 25 1 63-28 69-28 6-48-10-50-34-1-11 7-21 18-30Z" fill="#f8f7ef" />
    <path d="M29 76c-14 17-25 33-22 39 6 9 39-10 56-31L50 67c-7 1-14 3-21 9Z" fill="#f36a21" />
    <path d="M84 49c21 9 36 25 49 47-19 0-42-7-61-23Z" fill="#ec6a22" />
    <path d="M57 49c12-12 31-17 44-10 0 18-15 37-35 41-17 3-28-8-9-31Z" fill="#f47b2c" />
    <path d="M38 44c15-18 39-20 53-6-12 13-30 16-53 6Z" fill="#fff" />
    <circle cx="78" cy="44" r="6" fill="#101010" />
    <circle cx="80" cy="42" r="2" fill="#fff" />
    <path d="M88 41c12-2 21 1 26 8-8 6-16 8-26 6Z" fill="#f3722c" />
    <path d="M87 40c9-8 18-8 27-3-5 7-12 10-23 11Z" fill="#fff" />
    <path d="M41 103l-4 15M63 102l6 15" stroke="#a4652a" stroke-width="3" stroke-linecap="round" />
  </svg>

  <svg
    v-else-if="type === 'main-orange'"
    class="visual visual-main-orange"
    viewBox="0 0 520 300"
    role="img"
    aria-label="实时监控鹦鹉"
  >
    <ellipse cx="222" cy="258" rx="118" ry="14" fill="#caa77f" opacity=".24" />
    <path d="M155 95c26-71 118-78 159-16 45 68 5 172-74 188-71 15-125-27-129-94-2-26 18-55 44-78Z" fill="#f7f6ee" />
    <path d="M94 196c-43 49-81 99-72 113 15 24 111-36 160-93l-44-48c-17 2-31 10-44 28Z" fill="#f36b22" />
    <path d="M309 111c75 20 137 62 190 127-59 7-133-5-209-52Z" fill="#ef6e21" />
    <path d="M300 124c58 12 111 42 158 87-54-7-110-23-169-64Z" fill="#f48b2c" />
    <path d="M139 79c-39 11-66 38-78 80 48 7 91-14 113-55Z" fill="#1f1f1f" />
    <path d="M190 104c38-39 92-51 126-27 0 47-38 100-92 110-47 9-78-20-34-83Z" fill="#f4772c" />
    <path d="M126 82c33-42 90-45 126-10-31 34-81 42-126 10Z" fill="#ffffff" />
    <path d="M174 119c28 24 33 62 13 92-42 4-80-10-101-37 8-35 36-55 88-55Z" fill="#1f1f1f" />
    <circle cx="223" cy="77" r="15" fill="#101010" />
    <circle cx="228" cy="72" r="5" fill="#fff" />
    <path d="M249 69c33-7 57 1 72 24-21 16-45 21-72 17Z" fill="#f3722c" />
    <path d="M247 68c24-22 51-22 72-9-14 19-34 27-64 29Z" fill="#ffffff" />
    <path d="M331 133c35 14 68 40 97 75" fill="none" stroke="#365c28" stroke-width="8" stroke-linecap="round" stroke-dasharray="27 18" />
    <path d="M61 250c44-11 80-29 112-61" fill="none" stroke="#315822" stroke-width="8" stroke-linecap="round" stroke-dasharray="30 18" />
    <path d="M178 258l-9 39M232 258l16 38" stroke="#9a5f2d" stroke-width="7" stroke-linecap="round" />
    <path d="M151 297c28-5 51-2 69 9M224 297c34-6 59-3 78 7" stroke="#9a5f2d" stroke-width="6" stroke-linecap="round" />
    <path d="M227 219c32 5 66-5 91-30 4 45-47 70-91 30Z" fill="#d9d6cc" opacity=".65" />
  </svg>
</template>
