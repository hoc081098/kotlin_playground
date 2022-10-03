package com.hoc081098.kotlin_playground

const val ALPHABET_SIZE = 26

data class TrieNode(
  val children: MutableList<TrieNode?> = MutableList(ALPHABET_SIZE) { null },
  var isEndOfWord: Boolean = false
)

private inline val TrieNode.isEmpty: Boolean get() = children.all { it === null }

class Trie {
  private val root = TrieNode()

  /**
   * @return true if root has no children, else false
   */
  val isEmpty: Boolean
    get() = root.isEmpty

  /**
   * If not present, inserts [key] into trie
   * If the key is prefix of trie node, just marks leaf node
   */
  fun insert(key: String) {
    key
      .map { it - 'a' }
      .fold(root) { acc, index ->
        acc.children[index]
          ?: TrieNode().also { acc.children[index] = it }
      }
      .isEndOfWord = true
  }

  /**
   * @return `true` if [key] presents in trie, else `false`
   */
  fun search(key: String): Boolean {
    var last = null as TrieNode?

    return key
      .asSequence()
      .map { it - 'a' }
      .scan(root as TrieNode?) { acc, index -> acc?.children?.get(index) }
      .onEach { last = it }
      .all { it != null } && last?.isEndOfWord == true
  }

  fun remove(key: String) {
    remove(root, key, 0)
  }

  private companion object {
    private fun remove(node: TrieNode?, key: String, depth: Int): TrieNode? {
      if (node === null) {
        return null
      }

      if (key.length == depth) {
        node.isEndOfWord = false

        return if (node.isEmpty) {
          null
        } else {
          node
        }
      }

      val index = key[depth] - 'a'
      node.children[index] = remove(node.children[index], key, depth + 1)

      return if (node.isEmpty && !node.isEndOfWord) {
        null
      } else {
        node
      }
    }
  }
}

@Suppress("UNCHECKED_CAST") // tricks with erased generics go here, do not repeat on reified platforms
internal inline fun <K, V, R> MutableMap<K, V>.mapValuesInPlace(f: (Map.Entry<K, V>) -> R): MutableMap<K, R> {
  entries.forEach {
    (it as MutableMap.MutableEntry<K, R>).setValue(f(it))
  }
  return (this as MutableMap<K, R>)
}

fun main() {
  listOf(1).groupingBy { it }.eachCount()

  val t = Trie()
  println("isEmpty: ${t.isEmpty}")

  println("-".repeat(32))

  arrayOf("the", "a", "there", "answer", "any", "by", "bye", "their")
    .forEach(t::insert)
  println("isEmpty: ${t.isEmpty}")

  arrayOf("the", "these", "their", "thaw", "th")
    .forEach { println("$it -> ${t.search(it)}") }

  println("-".repeat(32))

  t.remove("th")
  t.remove("the")
  arrayOf("the", "these", "their", "thaw", "th")
    .forEach { println("$it -> ${t.search(it)}") }
}