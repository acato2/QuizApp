package ba.etf.rma22.projekat.viewmodel


import ba.etf.rma22.projekat.data.models.Grupa
import ba.etf.rma22.projekat.data.models.Istrazivanje
import ba.etf.rma22.projekat.data.repositories.GrupaRepository
import ba.etf.rma22.projekat.data.repositories.IstrazivanjeIGrupaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GrupaViewModel {
    val scope = CoroutineScope(Job() + Dispatchers.Main)
    fun getGroupsByIstrazivanje(onSuccess: (ankete: List<Grupa>) -> Unit,
                                onError: () -> Unit,nazivIstrazivanja : String){
            scope.launch{
                val result = GrupaRepository.getGroupsByIstrazivanje(nazivIstrazivanja)
                when (result) {
                    is List<Grupa> -> onSuccess?.invoke(result)
                    else-> onError?.invoke()
                }
            }

    }

    fun upisUGrupu(idGrupe: Int, onSuccess: (Boolean) -> Unit, onError: () -> Unit) {
        scope.launch {
            val upisan = IstrazivanjeIGrupaRepository.upisiUGrupu(idGrupe)
            when (upisan) {
                is Boolean -> onSuccess?.invoke(upisan)
                else -> onError?.invoke()
            }
        }
    }
}