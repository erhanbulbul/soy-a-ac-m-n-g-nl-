package com.example.util

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    TURKISH("tr", "Türkçe", "🇹🇷"),
    ENGLISH("en", "English", "🇺🇸"),
    SPANISH("es", "Español", "🇪🇸"),
    GERMAN("de", "Deutsch", "🇩🇪"),
    FRENCH("fr", "Français", "🇫🇷"),
    ARABIC("ar", "العربية", "🇸🇦")
}

object LanguageManager {
    private val translations = mapOf(
        "app_title" to mapOf(
            "tr" to "SoyAğacı Miras Günlüğü",
            "en" to "Ancestry Legacy Diary",
            "es" to "Diario de Legado Familiar",
            "de" to "Stammbaum & Tagebuch",
            "fr" to "Journal de Légat Familial",
            "ar" to "مذكرات شجرة العائلة"
        ),
        "welcome" to mapOf(
            "tr" to "Hoş Geldiniz",
            "en" to "Welcome",
            "es" to "Bienvenido",
            "de" to "Willkommen",
            "fr" to "Bienvenue",
            "ar" to "مرحباً بك"
        ),
        "user_id" to mapOf(
            "tr" to "Takip Kodu (ID)",
            "en" to "User ID Code",
            "es" to "Código ID",
            "de" to "Benutzer-ID",
            "fr" to "Code ID",
            "ar" to "رمز الهوية"
        ),
        "family_tree" to mapOf(
            "tr" to "Soya Ağacı",
            "en" to "Family Tree",
            "es" to "Árbol Genealógico",
            "de" to "Stammbaum",
            "fr" to "Arbre Généalogique",
            "ar" to "شجرة العائلة"
        ),
        "journal" to mapOf(
            "tr" to "Günlük",
            "en" to "Journal",
            "es" to "Diario",
            "de" to "Tagebuch",
            "fr" to "Journal",
            "ar" to "المذكرات"
        ),
        "will_vault" to mapOf(
            "tr" to "Vasiyet & Miras",
            "en" to "Will Vault",
            "es" to "Bóveda Testamento",
            "de" to "Testament Tresor",
            "fr" to "Coffre Testament",
            "ar" to "خزنة الوصية"
        ),
        "chat" to mapOf(
            "tr" to "Sohbet",
            "en" to "Chat",
            "es" to "Chat",
            "de" to "Chat",
            "fr" to "Discussion",
            "ar" to "المحادثة"
        ),
        "settings" to mapOf(
            "tr" to "Ayarlar",
            "en" to "Settings",
            "es" to "Ajustes",
            "de" to "Einstellungen",
            "fr" to "Paramètres",
            "ar" to "الإعدادات"
        ),
        "profile" to mapOf(
            "tr" to "Profil / Giriş",
            "en" to "Profile / Login",
            "es" to "Perfil / Acceso",
            "de" to "Profil / Login",
            "fr" to "Profil / Connexion",
            "ar" to "الملف الشخصي / الدخول"
        ),
        "add_member" to mapOf(
            "tr" to "Aile Üyesi Ekle (6 Haneli ID)",
            "en" to "Add Member (6-Digit ID)",
            "es" to "Agregar Miembro (ID 6 dígitos)",
            "de" to "Mitglied hinzufügen (6-stellige ID)",
            "fr" to "Ajouter Membre (ID 6 chiffres)",
            "ar" to "إضافة عضو (رمز 6 أرقام)"
        ),
        "write_entry" to mapOf(
            "tr" to "Günlük Yaz",
            "en" to "New Journal Entry",
            "es" to "Nueva Entrada de Diario",
            "de" to "Neuer Tagebucheintrag",
            "fr" to "Nouvelle Entrée du Journal",
            "ar" to "كتابة يوميات yeni"
        ),
        "font_color" to mapOf(
            "tr" to "Yazı Rengi",
            "en" to "Font Color",
            "es" to "Color de Letra",
            "de" to "Schriftfarbe",
            "fr" to "Couleur de Police",
            "ar" to "لون الخط"
        ),
        "voice_input" to mapOf(
            "tr" to "Sesle Yazma & Konuşma",
            "en" to "Voice Input & Record",
            "es" to "Entrada por Voz",
            "de" to "Spracheingabe",
            "fr" to "Entrée Vocale",
            "ar" to "الإدخال الصوتي"
        ),
        "export_pdf" to mapOf(
            "tr" to "Günlüğü PDF İndir",
            "en" to "Export Journal as PDF",
            "es" to "Exportar a PDF",
            "de" to "Als PDF exportieren",
            "fr" to "Exporter en PDF",
            "ar" to "تصدير المذكرات كـ PDF"
        ),
        "safety_check" to mapOf(
            "tr" to "24 Saat Varlık Kontrolü",
            "en" to "24-Hour Safety Check",
            "es" to "Verificación 24 Horas",
            "de" to "24-Stunden-Sicherheitsprüfung",
            "fr" to "Vérification 24 Heures",
            "ar" to "فحص السلامة خلال 24 ساعة"
        ),
        "status_alive" to mapOf(
            "tr" to "Hayattayım & Güvendeyim",
            "en" to "I am Safe & Active",
            "es" to "Estoy Seguro y Activo",
            "de" to "Ich bin sicher",
            "fr" to "Je suis en sécurité",
            "ar" to "أنا بخير وبصحة جيدة"
        ),
        "deceased_notice" to mapOf(
            "tr" to "Vefat Durumu & Anı Avatarı",
            "en" to "Deceased Status & AI Memory Avatar",
            "es" to "Estado Fallecido y Avatar IA",
            "de" to "Verstorben-Status & KI-Avatar",
            "fr" to "Statut Décédé & Avatar IA",
            "ar" to "حالة الوفاة والصورة الرمزية الذكية"
        ),
        "chat_with_avatar" to mapOf(
            "tr" to "Dijital Anı Avatarı ile Sohbet Et",
            "en" to "Chat with Digital Memory Avatar",
            "es" to "Chatear con Avatar Digital",
            "de" to "Mit KI-Avatar chatten",
            "fr" to "Discuter avec l'Avatar Mémoire",
            "ar" to "التحدث مع الرمز الرقمي للفقيد"
        ),
        "delete_account" to mapOf(
            "tr" to "Hesabı Sil",
            "en" to "Delete Account",
            "es" to "Eliminar Cuenta",
            "de" to "Konto löschen",
            "fr" to "Supprimer le Compte",
            "ar" to "حذف الحساب"
        ),
        "is_deceased_member" to mapOf(
            "tr" to "Vefat Etmiş Akraba (Manuel Ekle)",
            "en" to "Deceased Relative (Manual Entry)",
            "es" to "Fallecido (Registro Manual)",
            "de" to "Verstorbener Verwandter (Manuell)",
            "fr" to "Parent Décédé (Entrée Manuelle)",
            "ar" to "قريب متوفى (إضافة يدوي)"
        ),
        "deceased_member_hint" to mapOf(
            "tr" to "Uygulamayı kullanamayacak olan merhum aile büyüklerinizi manuel ekleyebilirsiniz.",
            "en" to "You can manually add deceased family members who cannot use the app.",
            "es" to "Puede agregar manualmente a los parientes fallecidos que no usaron la app.",
            "de" to "Sie können verstorbene Familienmitglieder manuell hinzufügen.",
            "fr" to "Vous pouvez ajouter manuellement les membres décédés de la famille.",
            "ar" to "يمكنك إضافة أفراد العائلة المتوفين يدوياً."
        ),
        "years_lived" to mapOf(
            "tr" to "Doğum - Vefat Yılı (Örn: 1940 - 2018)",
            "en" to "Birth - Death Year (e.g. 1940 - 2018)",
            "es" to "Año Nacimiento - Fallecimiento (ej. 1940 - 2018)",
            "de" to "Geburts- und Todesjahr (z.B. 1940 - 2018)",
            "fr" to "Années Naissance - Décès (ex. 1940 - 2018)",
            "ar" to "سنة الميلاد - الوفاة (مثال: 1940 - 2018)"
        ),
        "social_login" to mapOf(
            "tr" to "Sosyal Hesap İle Giriş Yap",
            "en" to "Sign In with Social Account",
            "es" to "Iniciar con Red Social",
            "de" to "Mit Social Media anmelden",
            "fr" to "Se connecter avec un Réseau Social",
            "ar" to "تسجيل الدخول باستخدام حساب اجتماعي"
        ),
        "user_login_settings" to mapOf(
            "tr" to "Kullanıcı Girişi & Profil Ayarları",
            "en" to "User Login & Profile Settings",
            "es" to "Inicio de Sesión y Perfil",
            "de" to "Benutzeranmeldung & Profil",
            "fr" to "Connexion & Profil",
            "ar" to "تسجيل الدخول وإعدادات الملف"
        ),
        "social_login_sub" to mapOf(
            "tr" to "Google, Facebook, Twitter, LinkedIn girişi & Fotoğraf Yükle",
            "en" to "Google, Facebook, Twitter, LinkedIn login & Upload Photo",
            "es" to "Acceso Google, Facebook, Twitter, LinkedIn y foto",
            "de" to "Google, Facebook, Twitter, LinkedIn Login & Foto-Upload",
            "fr" to "Connexion Google, Facebook, Twitter, LinkedIn & photo",
            "ar" to "تسجيل Google, Facebook, Twitter, LinkedIn وتحميل الصورة"
        ),
        "open" to mapOf(
            "tr" to "Aç",
            "en" to "Open",
            "es" to "Abrir",
            "de" to "Öffnen",
            "fr" to "Ouvrir",
            "ar" to "فتح"
        ),
        "safety_check_sub" to mapOf(
            "tr" to "Günlük varlık kontrolü: 24 saat içinde bu butona basarak hayatta olduğunuzu doğrulayın.",
            "en" to "Daily safety check: Confirm you are active within 24 hours by tapping this button.",
            "es" to "Control de seguridad: Confirme que está activo presionando este botón.",
            "de" to "Tägliche Sicherheitsprüfung: Bestätigen Sie Ihre Aktivität innerhalb von 24 Stunden.",
            "fr" to "Vérification quotidienne: Confirmez votre activité dans les 24 heures.",
            "ar" to "فحص السلامة اليومي: تأكيد نشاطك خلال 24 ساعة بالضغط على هذا الزر."
        ),
        "deceased_safety_sub" to mapOf(
            "tr" to "Sistem 24 saat cevapsız kalan bildirim sonucu vefat durumunu onayladı. Dijital anı avatarı aktif.",
            "en" to "System confirmed status due to unanswered 24h check. Digital memory avatar activated.",
            "es" to "El sistema confirmó estado tras 24h sin respuesta. Avatar activado.",
            "de" to "System hat den Status nach 24h ohne Antwort bestätigt. KI-Avatar aktiv.",
            "fr" to "Statut confirmé après 24h sans réponse. Avatar activé.",
            "ar" to "تأكيد النظام بعد عدم الرد لمدة 24 ساعة. تم تفعيل الرمز الرقمي."
        ),
        "deceased_simulation" to mapOf(
            "tr" to "Vefat Durumu Simülasyonu (Test)",
            "en" to "Deceased Status Simulation (Test)",
            "es" to "Simulación de Estado (Prueba)",
            "de" to "Status-Simulation (Test)",
            "fr" to "Simulation de Statut (Test)",
            "ar" to "محاكاة حالة الوفاة (اختبار)"
        ),
        "hero_title" to mapOf(
            "tr" to "Sonsuz Aile Mirası",
            "en" to "Endless Family Legacy",
            "es" to "Legado Familiar Eterno",
            "de" to "Ewiges Familienerbe",
            "fr" to "Héritage Familial Éternel",
            "ar" to "إرث العائلة الخالد"
        ),
        "hero_sub" to mapOf(
            "tr" to "Nesiller boyu sürecek dijital hatıralarınız güvende.",
            "en" to "Your digital memories lasting for generations are secure.",
            "es" to "Sus recuerdos digitales seguros para generaciones.",
            "de" to "Ihre digitalen Erinnerungen für Generationen geschützt.",
            "fr" to "Vos souvenirs numériques sécurisés pour des générations.",
            "ar" to "ذكرياتك الرقمية آمنة عبر الأجيال."
        ),
        "dashboard_access" to mapOf(
            "tr" to "Erişim Paneli",
            "en" to "Access Dashboard",
            "es" to "Panel de Acceso",
            "de" to "Zugriffspanel",
            "fr" to "Tableau de Bord",
            "ar" to "لوحة الوصول"
        ),
        "tree_sub" to mapOf(
            "tr" to "Soy ağacı & 6 haneli kod ekle",
            "en" to "Family tree & add 6-digit code",
            "es" to "Árbol genealógico y código 6 dígitos",
            "de" to "Stammbaum & 6-stelligen Code hinzufügen",
            "fr" to "Arbre & ajouter code à 6 chiffres",
            "ar" to "شجرة العائلة وإضافة رمز 6 أرقام"
        ),
        "journal_sub" to mapOf(
            "tr" to "Günlük, sesli yazma & PDF indir",
            "en" to "Journal, voice input & export PDF",
            "es" to "Diario, entrada de voz y PDF",
            "de" to "Tagebuch, Spracheingabe & PDF",
            "fr" to "Journal, saisie vocale & PDF",
            "ar" to "مذكرات وإدخال صوتي وتصدير PDF"
        ),
        "will_sub" to mapOf(
            "tr" to "Vasiyet & Kamera Belge Yükle",
            "en" to "Will & Camera Document Upload",
            "es" to "Testamento y fotos de documentos",
            "de" to "Testament & Dokumenten-Upload",
            "fr" to "Testament & envoi de documents",
            "ar" to "الوصية وتحميل المستندات بالكاميرا"
        ),
        "chat_sub" to mapOf(
            "tr" to "Aile mesajlaşma & Anı Avatarı",
            "en" to "Family messaging & Memory Avatar",
            "es" to "Mensajería familiar y Avatar",
            "de" to "Familien-Messaging & KI-Avatar",
            "fr" to "Messagerie familiale & Avatar",
            "ar" to "الدردشة العائلية والرمز الذكي"
        ),
        "settings_sub" to mapOf(
            "tr" to "Profil, Fotoğraf, 6 Dil & Sosyal Girişler",
            "en" to "Profile, Photo, 6 Languages & Social Logins",
            "es" to "Perfil, foto, 6 idiomas y accesos sociales",
            "de" to "Profil, Foto, 6 Sprachen & Logins",
            "fr" to "Profil, photo, 6 langues & connexions",
            "ar" to "الملف، الصورة، 6 لغات وتسجيلات الدخول"
        ),
        "select_language" to mapOf(
            "tr" to "Uygulama Dilini Seçin",
            "en" to "Select App Language",
            "es" to "Seleccionar Idioma de la App",
            "de" to "App-Sprache auswählen",
            "fr" to "Choisir la Langue",
            "ar" to "اختر لغة التطبيق"
        ),
        "close" to mapOf(
            "tr" to "Kapat",
            "en" to "Close",
            "es" to "Cerrar",
            "de" to "Schließen",
            "fr" to "Fermer",
            "ar" to "إغلاق"
        ),
        "copied_id" to mapOf(
            "tr" to "ID Kopyalandı",
            "en" to "ID Copied",
            "es" to "ID Copiado",
            "de" to "ID kopiert",
            "fr" to "ID Copié",
            "ar" to "تم نسخ المعرف"
        ),
        "add_member_btn" to mapOf(
            "tr" to "Aile Üyesi Ekle",
            "en" to "Add Family Member",
            "es" to "Añadir Miembro",
            "de" to "Familienmitglied hinzufügen",
            "fr" to "Ajouter un Membre",
            "ar" to "إضافة فرد من العائلة"
        ),
        "enter_code" to mapOf(
            "tr" to "6 Haneli Kullanıcı ID Kodu",
            "en" to "6-Digit User ID Code",
            "es" to "Código ID de 6 dígitos",
            "de" to "6-stelliger Benutzer-ID-Code",
            "fr" to "Code ID à 6 chiffres",
            "ar" to "رمز ID المكون من 6 أرقام"
        ),
        "relative_name" to mapOf(
            "tr" to "Akraba Adı Soyadı",
            "en" to "Relative Full Name",
            "es" to "Nombre Completo del Pariente",
            "de" to "Vollständiger Name des Verwandten",
            "fr" to "Nom complet du parent",
            "ar" to "اسم القريب الكامل"
        ),
        "relationship" to mapOf(
            "tr" to "Akrabalık Derecesi (Baba, Anne, Kardeş vb.)",
            "en" to "Relationship (Father, Mother, Sibling, etc.)",
            "es" to "Parentesco (Padre, Madre, Hermano, etc.)",
            "de" to "Verwandtschaft (Vater, Mutter, Geschwister)",
            "fr" to "Lien de parenté (Père, Mère, Frère, etc.)",
            "ar" to "صلة القرابة (أب، أم، أخ، إلخ)"
        ),
        "notes" to mapOf(
            "tr" to "Özel Notlar / Anılar",
            "en" to "Special Notes / Memories",
            "es" to "Notas Especiales / Recuerdos",
            "de" to "Besondere Notizen / Erinnerungen",
            "fr" to "Notes Particulières / Souvenirs",
            "ar" to "ملاحظات خاصة / ذكريات"
        ),
        "generation_grand" to mapOf(
            "tr" to "Büyükler (Dede / Nene / Anneanne / Babaanne)",
            "en" to "Grandparents & Ancestors",
            "es" to "Abuelos y Antepasados",
            "de" to "Großeltern & Vorfahren",
            "fr" to "Grands-parents & Ancêtres",
            "ar" to "الأجداد والأسلاف"
        ),
        "generation_parents" to mapOf(
            "tr" to "Ebeveynler & Akrabalar (Anne / Baba / Teyze / Amca)",
            "en" to "Parents & Relatives (Mother / Father / Aunt / Uncle)",
            "es" to "Padres y Parientes (Madre / Padre / Tía / Tío)",
            "de" to "Eltern & Verwandte (Mutter / Vater / Tante / Onkel)",
            "fr" to "Parents & Famille (Mère / Père / Tante / Oncle)",
            "ar" to "الوالدان والأقارب (أم / أب / خالة / عم)"
        ),
        "generation_us" to mapOf(
            "tr" to "Bizim Nesil (Ben & Kardeşler & Eş)",
            "en" to "Our Generation (Me & Siblings & Spouse)",
            "es" to "Nuestra Generación (Yo y Hermanos y Cónyuge)",
            "de" to "Unsere Generation (Ich & Geschwister & Ehepartner)",
            "fr" to "Notre Génération (Moi & Frères/Sœurs & Conjoint)",
            "ar" to "جيلنا (أنا والإخوة والزوج/الزوجة)"
        ),
        "generation_children" to mapOf(
            "tr" to "Çocuklar & Gelecek Nesil",
            "en" to "Children & Next Generation",
            "es" to "Hijos y Próxima Generación",
            "de" to "Kinder & Nächste Generation",
            "fr" to "Enfants & Prochaine Génération",
            "ar" to "الأبناء والجيل القادم"
        ),
        "no_members" to mapOf(
            "tr" to "Henüz eklenecek üye bulunamadı. '+' butonuna basarak 6 haneli kod ile akraba ekleyebilirsiniz.",
            "en" to "No members added yet. Tap '+' to add a relative using their 6-digit ID.",
            "es" to "Aún no hay miembros. Presione '+' para agregar un pariente con su ID de 6 dígitos.",
            "de" to "Noch keine Mitglieder. Tippen Sie auf '+', um Verwandte mit ihrer 6-stelligen ID hinzuzufügen.",
            "fr" to "Aucun membre. Appuyez sur '+' pour ajouter un parent avec son code à 6 chiffres.",
            "ar" to "لم يتم إضافة أعضاء بعد. اضغط على '+' لإضافة قريب باستخدام الرمز الخاص به."
        ),
        "no_journal_entries" to mapOf(
            "tr" to "Henüz kaydedilmiş günlük anınız bulunmuyor. Yeni anı eklemek için '+' butonuna dokunun.",
            "en" to "No journal entries found. Tap '+' to record a new memory.",
            "es" to "No hay entradas de diario. Toque '+' para grabar un recuerdo.",
            "de" to "Keine Tagebucheinträge. Tippen Sie auf '+', um eine Erinnerung zu speichern.",
            "fr" to "Aucune entrée. Appuyez sur '+' pour enregistrer un souvenir.",
            "ar" to "لا توجد مذكرات محفوطة. اضغط على '+' لإضافة ذكرى جديدة."
        ),
        "add_photo" to mapOf(
            "tr" to "Fotoğraf Ekle",
            "en" to "Add Photo",
            "es" to "Añadir Foto",
            "de" to "Foto hinzufügen",
            "fr" to "Ajouter une Photo",
            "ar" to "إضافة صورة"
        ),
        "voice_dictation" to mapOf(
            "tr" to "🎙️ Sesli Dikte (Konuşarak Yaz)",
            "en" to "🎙️ Voice Dictation (Speak to Write)",
            "es" to "🎙️ Dictado por Voz (Hablar para Escribir)",
            "de" to "🎙️ Sprachdiktat (Sprechen zum Schreiben)",
            "fr" to "🎙️ Dictée Vocale (Parler pour Écrire)",
            "ar" to "🎙️ الإملاء الصوتي (تحدث للتكابة)"
        ),
        "save_entry" to mapOf(
            "tr" to "Günlüğe Şifreli Kaydet",
            "en" to "Save Encrypted Entry",
            "es" to "Guardar Entrada Encriptada",
            "de" to "Verschlüsselten Eintrag speichern",
            "fr" to "Enregistrer l'Entrée Chiffrée",
            "ar" to "حفظ المذكرة المشفرة"
        ),
        "update_entry" to mapOf(
            "tr" to "Günlüğü Güncelle & Kaydet",
            "en" to "Update & Save Entry",
            "es" to "Actualizar y Guardar",
            "de" to "Eintrag aktualisieren & speichern",
            "fr" to "Mettre à jour et Enregistrer",
            "ar" to "تحديث وحفظ المذكرة"
        ),
        "save" to mapOf(
            "tr" to "Kaydet",
            "en" to "Save",
            "es" to "Guardar",
            "de" to "Speichern",
            "fr" to "Enregistrer",
            "ar" to "حفظ"
        ),
        "cancel" to mapOf(
            "tr" to "İptal",
            "en" to "Cancel",
            "es" to "Cancelar",
            "de" to "Abbrechen",
            "fr" to "Annuler",
            "ar" to "إلغاء"
        ),
        "logout" to mapOf(
            "tr" to "Çıkış Yap / Oturumu Kapat",
            "en" to "Log Out",
            "es" to "Cerrar Sesión",
            "de" to "Abmelden",
            "fr" to "Se Déconnecter",
            "ar" to "تسجيل الخروج"
        ),
        "sos_bluetooth_title" to mapOf(
            "tr" to "Çevrimdışı Deprem SOS (Bluetooth Mesh)",
            "en" to "Offline Earthquake SOS (Bluetooth Mesh)",
            "es" to "SOS Terremoto Fuera de Línea (Bluetooth)",
            "de" to "Offline Erdbeben SOS (Bluetooth)",
            "fr" to "SOS Séisme Hors-Ligne (Bluetooth)",
            "ar" to "SOS طوارئ الزلازل بدون إنترنت (بلوتوث)"
        ),
        "sos_bluetooth_sub" to mapOf(
            "tr" to "İnternet ve şebeke olmadan yakındaki kullanıcılara sesli mesaj & acil sinyal gönderin.",
            "en" to "Send voice notes & emergency signals to nearby users without internet or cell service.",
            "es" to "Envía notas de voz y señales de emergencia a usuarios cercanos sin internet ni cobertura.",
            "de" to "Senden Sie Sprachnachrichten und Notsignale an Benutzer in der Nähe ohne Internet.",
            "fr" to "Envoyez des messages vocaux et signaux d'urgence aux utilisateurs proches sans réseau.",
            "ar" to "إرسال رسائل صوتية وإشارات طوارئ للمستخدمين القريبين بدون شبكة أو إنترنت."
        ),
        "sos_beacon_status" to mapOf(
            "tr" to "Bluetooth SOS Yayın Durumu",
            "en" to "Bluetooth SOS Beacon Status",
            "es" to "Estado de Baliza Bluetooth SOS",
            "de" to "Bluetooth SOS Baken-Status",
            "fr" to "Statut de la Balise Bluetooth SOS",
            "ar" to "حالة منارة الطوارئ بلوتوث"
        ),
        "sos_beacon_active" to mapOf(
            "tr" to "AKTİF — Yakındaki Cihazlara Acil Durum Sinyali Yayınlanıyor (10-100m)",
            "en" to "ACTIVE — Emergency Beacon Broadcasting to Nearby Devices (10-100m)",
            "es" to "ACTIVO — Baliza de Emergencia Emitiendo a Dispositivos Cercanos (10-100m)",
            "de" to "AKTIV — Notbake sendet an Geräte in der Nähe (10-100m)",
            "fr" to "ACTIF — Balise d'Urgence Émise aux Appareils Proches (10-100m)",
            "ar" to "نشط — يتم إرسال إشارة الطوارئ للأجهزة القريبة (10-100m)"
        ),
        "sos_beacon_inactive" to mapOf(
            "tr" to "Pasif — Sinyal Yayınlanmıyor",
            "en" to "Inactive — Not Broadcasting",
            "es" to "Inactivo — No transmitiendo",
            "de" to "Inaktiv — Sendet nicht",
            "fr" to "Inactif — Pas d'émission",
            "ar" to "غير نشط — لا يتم الإرسال"
        ),
        "sos_record_voice" to mapOf(
            "tr" to "Acil Durum Ses Kaydı Al",
            "en" to "Record Emergency Voice Note",
            "es" to "Grabar Nota de Voz de Emergencia",
            "de" to "Notfall-Sprachnachricht aufnehmen",
            "fr" to "Enregistrer un Message Vocal d'Urgence",
            "ar" to "تسجيل رسالة صوتية للطوارئ"
        ),
        "sos_broadcast_voice" to mapOf(
            "tr" to "Bluetooth Mesh İle Yakındaki Kullanıcılara Yayınla",
            "en" to "Broadcast via Bluetooth Mesh to Nearby Users",
            "es" to "Emitir por Bluetooth Mesh a Usuarios Cercanos",
            "de" to "Über Bluetooth Mesh an Benutzer in der Nähe senden",
            "fr" to "Diffuser via Bluetooth Mesh aux Utilisateurs Proches",
            "ar" to "البث عبر شبكة البلوتوث للمستخدمين القريبين"
        ),
        "sos_mesh_relay" to mapOf(
            "tr" to "Mesh Akıllı Röle: Sinyalinizi alan yakındaki cihaz internete bağlandığı an sesli mesajınızı Arama Kurtarma ekiplerine otomatik iletir.",
            "en" to "Mesh Smart Relay: The nearby device receiving your signal will automatically forward your voice message to Search & Rescue as soon as it gains internet.",
            "es" to "Relé Inteligente Mesh: El dispositivo cercano que reciba tu señal la reenviará a Rescate en cuanto obtenga internet.",
            "de" to "Mesh Smart Relay: Das empfangende Gerät leitet Ihre Sprachnachricht an den Rettungsdienst weiter, sobald es Internet hat.",
            "fr" to "Relais Intelligent Mesh: L'appareil qui reçoit votre signal transmettra votre message aux secours dès qu'il aura du réseau.",
            "ar" to "ترحيل الشبكة الذكي: الجاهز القريب الذي يستقبل إشارتك سينقل رسالتك الصوتية إلى فرق الإنقاذ فور توفر الإنترنت."
        )
    )

    fun getString(key: String, langCode: String): String {
        val keyMap = translations[key] ?: return key
        return keyMap[langCode] ?: keyMap["tr"] ?: keyMap["en"] ?: key
    }
}
