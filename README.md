<div align="center">

# ☄️ ChaosRingEvents
### *Türkiye Minecraft Sektöründe Bir İlk: Dinamik Anomali Çemberi ve Çevresel Efekt Sistemi*

[![Java](https://img.shields.io/badge/Java-17%20%7C%2021-orange?style=for-the-badge&logo=java)](https://www.oracle.com/java/)
[![Paper](https://img.shields.io/badge/PaperAPI-1.20.2%20--%201.20.4-blue?style=for-the-badge&logo=minecraft)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](LICENSE)

</div>

---

## 📌 Proje Hakkında
**ChaosRingEvents**, klasik "Daireden Son Çıkan Kazanır / Last Man Standing" etkinliklerindeki monotonluğu ve sadece düz alanda birbirine vurma mantığını kökten değiştirmek için geliştirilmiş **yeni nesil bir anomali ve çevre yönetimi eklentisidir**. Çember daraldıkça haritada rastgele bölgeleri tehlikeli birer "Anomali Alanı"na dönüştürerek oyuncuları sadece PVP yetenekleriyle değil, anlık durumlara adapte olma refleksleriyle sınar.

---

## 🚀 Öne Çıkan Özellikler

| Özellik | Açıklama | Teknik Altyapı |
| :--- | :--- | :--- |
| **Dinamik Anomali Bölgeleri** | Çember küçülürken haritanın rastgele noktalarında anlık çevresel olaylar tetiklenir. | Asenkron Görev Yönetimi (`BukkitRunnable`) |
| **Yerçekimi Anomalisi** | Belirli bölgelerde yerçekimi düşer, oyuncular stratejik olarak yükseklere zıplar. | Hız ve Konum Vektör Manipülasyonu |
| **Çevresel Tehlike Alanları** | Performansı düşürmeyen, optimize edilmiş meteor/patlama efektleri ve kaygan zemin mekanikleri. | Optimize Edilmiş Event Dinleyicileri |
| **Sıfır Lag (Optimizasyon)** | Sunucu TPS değerini korumak için `PlayerMoveEvent` ve harita taramaları optimize edilmiştir. | Hafif Bellek Mimarisi |

---

## ⚙️ Teknik Mimari ve Performans
* **Asenkron Hesaplama:** Çember daralmaları ve anomali koordinat hesaplamaları ana iş parçacığına (Main Thread) yük bindirmeden yürütülür.
* **Modüler Yapı:** Kolayca yönetilebilir Manager sınıfları ve event yapılandırması.
* **Paper API Optimizasyonu:** `1.20.2 - 1.20.4` sürüm aralığında tam performans ve kararlılık.

---

## 📦 Kurulum ve Kullanım

1. Eklentiyi sunucunuzun `plugins` klasörüne atın.
2. Sunucuyu başlatın ve `config.yml` üzerinden anomali oranlarını ve çember sürelerini düzenleyin.
3. `/chaos start` komutuyla etkinliği doğrudan başlatın.

```yaml
# Örnek Config Yapısı
settings:
  ring-shrink-speed: 1.5
  anomaly-spawn-chance: 35
  async-processing: true