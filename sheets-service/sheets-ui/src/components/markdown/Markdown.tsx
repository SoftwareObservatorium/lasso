import { useEffect, useState } from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import rehypeHighlight from 'rehype-highlight'

const Markdown = ({mdpath, children}: any) => {
    const [text, setText] = useState('')

    useEffect(() => {
        fetch(mdpath)
        .then((response) => response.text())
        .then((md) => {
            setText(md)
        })
    }, [])

    return (
        <>
            <ReactMarkdown rehypePlugins={[rehypeHighlight]} remarkPlugins={[remarkGfm]}>{text}</ReactMarkdown>
        </>
    )
}

export default Markdown