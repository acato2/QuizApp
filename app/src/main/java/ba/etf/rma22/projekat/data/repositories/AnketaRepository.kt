package ba.etf.rma22.projekat.data.repositories



import android.content.Context
import ba.etf.rma22.projekat.data.AppDatabase
import ba.etf.rma22.projekat.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*



class AnketaRepository {

    companion object {
        private lateinit var context: Context
        fun setContext(_context: Context) {
            context = _context;
        }
        lateinit var pokrenutaAnketa: Anketa
        var anketaTaken: AnketaTaken? = null
        var zavrseneAnkete = mutableListOf<Anketa>()

        init {

        }
        suspend fun getAll():List<Anketa>?{
            return withContext(Dispatchers.IO) {
                try{
                    var page1 = getAll(1)
                    var page2 = getAll(2)
                    var sveAnkete = mutableListOf<Anketa>()
                    sveAnkete.addAll(page1!!)
                    sveAnkete.addAll(page2!!)
                    try{
                        var baza = AppDatabase.getInstance(context)
                        baza.anketaDao().unesiAnkete(sveAnkete)
                        return@withContext sveAnkete
                    } catch (e: Exception){
                        return@withContext sveAnkete
                    }

                }catch (e:Exception){
                    try {
                        var baza = AppDatabase.getInstance(context)
                        var ankete = baza.anketaDao().getAllAnkete()
                        return@withContext  ankete
                    }catch (e:Exception){
                        return@withContext null
                    }

                }
            }
        }


        //sve ankete
        suspend fun getAll(offset: Int): List<Anketa>? {
            return withContext(Dispatchers.IO) {
                try {
                    var sveAnkete = ApiAdapter.retrofit.getAll(offset) //sve ankete
                    var povratni = mutableListOf<Anketa>() //pomocni
                    for (anketa in sveAnkete) { //prolazimo kroz sve ankete
                        val grupeAnkete =
                            ApiAdapter.retrofit.getGrupeZaAnketu(anketa.id)  //grupe te ankete
                        for (grupa in grupeAnkete) { //prolazimo kroz grupe ankete
                            val nazivIstrazivanja =
                                ApiAdapter.retrofit.getIstrazivanjaById(grupa.istrazivanjeId).naziv //dobijamo naziv istrazivanja za tu grupu
                            anketa.nazivIstrazivanja = nazivIstrazivanja //definisemo ostalo
                            anketa.nazivGrupe = grupa.nazivGrupe
                        }
                        povratni.add(anketa) //dodajemo tu anketu u pomocni
                    }
                    try {
                        var baza = AppDatabase.getInstance(context)
                        baza.anketaDao().unesiAnkete(povratni)
                        return@withContext povratni
                    }catch (e:Exception){
                        return@withContext povratni
                    }

                }catch (e:Exception){
                    try {
                        var baza = AppDatabase.getInstance(context)
                        var ankete = baza.anketaDao().getAllAnkete()
                        return@withContext ankete
                    }catch (e:Exception){
                        return@withContext null
                    }
                }



            }
        }

        //sve moje ankete
        suspend fun getUpisane(): List<Anketa>? {
            return withContext(Dispatchers.IO) {
                try{
                val acc = AccountRepository() //za id studenta
                val grupe =
                    ApiAdapter.retrofit.getUpisaneGrupe(AccountRepository.getHash()) //dobijamo upisane grupe
                var upisane = mutableListOf<Anketa>()
                for (grupa in grupe) {
                    var upisaneAnkete = ApiAdapter.retrofit.getUpisane(grupa.id)  //upisane ankete
                    val nazivIstrazivanja =
                        ApiAdapter.retrofit.getIstrazivanjaById(grupa.istrazivanjeId).naziv //naziv istrazivanja
                    for (upisana in upisaneAnkete) { //definisi naziv grupe i istrazivanja za upisane ankete
                        upisana.nazivGrupe = grupa.naziv
                        upisana.nazivIstrazivanja = nazivIstrazivanja
                        upisane.add(upisana)
                    }
                }
                //definisemo datum rada u anketama
                val poceteAnkete = TakeAnketaRepository.getPoceteAnkete()
                if (poceteAnkete != null) {
                    for (anketa in poceteAnkete) {
                        upisane.stream().forEach { x ->
                            if (x.id == anketa.AnketumId) x.datumRada = anketa.datumRada.toString()
                        }
                    }
                }

                return@withContext upisane
            }catch (e:Exception){
                return@withContext null

                }            }
        }


        //uradjene moje ankete
        suspend fun getDone(): List<Anketa> {
            return withContext(Dispatchers.IO) {
                val upisaneAnkete = getUpisane()
                var uradjeneAnkete = mutableListOf<Anketa>()
                for (anketa in upisaneAnkete!!) {
                    if(statusAnkete(anketa).equals("plava")){
                        uradjeneAnkete.add(anketa)
                    }
                }
                return@withContext uradjeneAnkete
            }
        }

        //buduce ankete te grupe
        suspend fun getFuture(): List<Anketa> {
            return withContext(Dispatchers.IO) {
                val upisaneAnkete = getUpisane()
                var buduceAnkete = mutableListOf<Anketa>()
                for (anketa in upisaneAnkete!!) {
                    if(statusAnkete(anketa).equals("zuta")){
                        buduceAnkete.add(anketa)
                    }
                }
                return@withContext buduceAnkete
            }
        }

        suspend fun getNotTaken(): List<Anketa> {
            return withContext(Dispatchers.IO) {
                val upisaneAnkete = getUpisane()
                var prosleAnkete = mutableListOf<Anketa>()
                for (anketa in upisaneAnkete!!) {
                    if(statusAnkete(anketa).equals("crvena")){
                        prosleAnkete.add(anketa)
                    }
                }
                return@withContext prosleAnkete
            }
        }
        //novo


        suspend fun getById(id: Int): Anketa? {
            return withContext(Dispatchers.IO) {
                try {
                    return@withContext ApiAdapter.retrofit.getById(id)
                } catch (e: Exception) {
                    return@withContext null
                }
            }
        }

        private fun statusAnkete(anketa: Anketa): String {

            var datumRada = anketa.datumRada
            var datumPocetka = anketa.datumPocetak
            var datumKraj = anketa.datumKraj


            val calender = Calendar.getInstance()

            val year =
                calender.get(Calendar.YEAR) - 1900  // jer koristimo date klasu moramo godinu oduzet od 1900
            val month = calender.get(Calendar.MONTH)
            val day = calender.get(Calendar.DAY_OF_MONTH)
            val currentDate: Date = Date(year, month, day)
            val boja: String
          /*  if (datumRada==null && datumPocetka.before(currentDate)) { //aktivna ali nije uradjena
                boja = "zelena"

            } else if (datumPocetka.after(currentDate) && datumRada==null ) { //nije aktivna nije uradjena
                boja = "zuta"
*/
             if (datumRada==null) { // anketa prosla i nije uradjena
                boja = "crvena"


            } else {
                boja = "plava"

            }


            return boja

        }

        fun dodajUZavrseneAnkete(pokrenutaAnketa: Anketa) {
            zavrseneAnkete.add(pokrenutaAnketa)

        }
     /*   fun stringToDate(date: String?) : Date{
            var formatter = SimpleDateFormat("yyyy-mm-dd")
            return formatter.parse(date)
        }*/
    }


}